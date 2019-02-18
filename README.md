# netty_TCP
基于netty的TCP服务器/客户端
> 在[上一篇中](https://blog.csdn.net/qq_24874939/article/details/86475285)介绍了基于netty4.x搭建一款灵活、稳健的TCP数据传输服务器，并处理了TCP通信中可能发生的的粘包、拆包问题（实际上是netty帮我们解决了）。能够在不改动解码器源码的前提下，通过Class.forName的作用，在反序列化的时候动态传入Class，实现任意Object的网络传输+灵活解码。但是处理不了集合类：Map、List等。
## 1.分析问题
回顾昨天的数据交互协议：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190115175509822.png)
**一个字节作为Header**+**4个字节作为长度len**+**len个字节的实际内容**+**一个字节的tail结束**
协议的实体类是这样的：

```java
public class TcpProtocol {
    private byte header=0x58;
    private int len;
    private byte [] data;
    private byte tail=0x63;
    }
```
在解码的时候反序列化了两次：

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof byte []){
            logger.debug("解码后的字节码："+new String((byte[]) msg,"UTF-8"));
            try {
                Object objectContainer = objectMapper.readValue((byte[]) msg, DTObject.class);//序列化成DTObject 读取FullClassName
                if (objectContainer instanceof DTObject){
                    DTObject data = (DTObject) objectContainer;
                    if (data.getClassName()!=null&&data.getObject().length>0){
                        Object object = objectMapper.readValue(data.getObject(), Class.forName(data.getClassName()));//获取到FullClassName后才成功反序列成真实要获取的对象
                        logger.info("收到实体对象："+object);
                    }
                }
            }catch (Exception e){
                logger.info("对象反序列化出现问题："+e);
            }

        }
```

> 由于没有考虑到List、Map的情况，因此这部分缺少判断类型的信息，解决办法有两种：1.是在协议中添加数据类型信息。2.是在DTObject中添加类型描述的字段。这里选择将数据类型放协议中去的方式。
## 2.方案一
将包含object类型的信息（map、list、普通object）添加到协议中后：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190115180727369.png)

 - 新的协议类中新增一个type字段：

```java
/**
 *type      0x51    0x52    0x53
 * mean:    object  list    map
 * */
public class TcpProtocol_2_0 {
    private byte header=0x58;
    private byte type;
    private int len;
    private byte [] data;
    private byte tail=0x63;
    }
```
 * 编码器也对应新增一个字节的内容：
 

```java
public class EncoderHandler_2_0 extends MessageToByteEncoder {
    private  Logger logger = Logger.getLogger(this.getClass());
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof TcpProtocol_2_0){
            TcpProtocol_2_0 protocol = (TcpProtocol_2_0) msg;
            out.writeByte(protocol.getHeader());
            out.writeByte(protocol.getType());//新增Type
            out.writeInt(protocol.getLen());
            out.writeBytes(protocol.getData());
            out.writeByte(protocol.getTail());
            logger.debug("数据编码成功："+out);
        }else {
            logger.info("不支持的数据协议："+msg.getClass()+"\t期待的数据协议类是："+ TcpProtocol_2_0.class);
        }
    }
}
```

 - 解码器解析顺序变成Header-->Type-->len--->data--->tail：
```java
public class DecoderHandler_2_0 extends ByteToMessageDecoder {
    //最小的数据长度：开头标准位1字节
    private static int MIN_DATA_LEN=6+1+1+1;
    //数据解码协议的开始标志
    private static byte PROTOCOL_HEADER=0x58;
    //数据解码协议的结束标志
    private static byte PROTOCOL_TAIL=0x63;
    private Logger logger = Logger.getLogger(this.getClass());
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes()>MIN_DATA_LEN){
            logger.debug("开始解码数据……");
            //标记读操作的指针
            in.markReaderIndex();
            byte header=in.readByte();
            if (header==PROTOCOL_HEADER){
                logger.debug("数据开头格式正确");
                //读取class类型
                byte type=in.readByte();
                    int dataLen=in.readInt();
                    if (dataLen<in.readableBytes()){
                        byte [] data=new byte[dataLen];
                        in.readBytes(data);
                        byte tail=in.readByte();
                        try {
                            if (tail==PROTOCOL_TAIL){
                                ObjectMapper objectMapper = ByteUtils.InstanceObjectMapper();
                                DTObject dtObject=objectMapper.readValue(data,DTObject.class);
                                Class<?> Type = Class.forName(dtObject.getClassName());
                                logger.debug("数据解码成功");
                                logger.debug("开始封装数据……");
                                if (type==ProtocolUtils.OBJ_TYPE){
                                    Object o = objectMapper.readValue(dtObject.getObject(), Type);
                                    out.add(o);
                                }else if (type==ProtocolUtils.MAP_TYPE){
                                    JavaType javaType= TypeFactory.defaultInstance().constructMapType(Map.class,String.class,Type);
                                    Object o = objectMapper.readValue(dtObject.getObject(), javaType);
                                    out.add(o);
                                }else if (type==ProtocolUtils.LIST_TYPE){
                                    JavaType javaType=TypeFactory.defaultInstance().constructCollectionType(List.class,Type);
                                    Object o = objectMapper.readValue(dtObject.getObject(), javaType);
                                    out.add(o);
                                }
                                //如果out有值，且in仍然可读，将继续调用decode方法再次解码in中的内容，以此解决粘包问题
                            }else {
                                logger.debug(String.format("数据解码协议结束标志位:%1$d [错误!]，期待的结束标志位是：%2$d",tail,PROTOCOL_TAIL));
                                return;
                            }
                        }catch (ClassNotFoundException e){
                            logger.error(String.format("反序列化对象的类找不到,注意包名匹配！ "));
                            return;
                        }catch (Exception e){
                            logger.error(e);
                            return;
                        }

                    }else{
                        logger.debug(String.format("数据长度不够，数据协议len长度为：%1$d,数据包实际可读内容为：%2$d正在等待处理拆包……",dataLen,in.readableBytes()));
                        in.resetReaderIndex();
                        /*
                         **结束解码，这种情况说明数据没有到齐，在父类ByteToMessageDecoder的callDecode中会对out和in进行判断
                         * 如果in里面还有可读内容即in.isReadable位true,cumulation中的内容会进行保留，，直到下一次数据到来，将两帧的数据合并起来，再解码。
                         * 以此解决拆包问题
                         */
                        return;
                    }
            }else {
                logger.debug("开头不对，可能不是期待的客服端发送的数，将自动略过这一个字节");
            }
        }else {
            logger.debug("数据长度不符合要求，期待最小长度是："+MIN_DATA_LEN+" 字节");
            return;
        }

    }
}
```

> 这里利用到了jackSon的JavaType来描述泛型，去反序列化Map和List类型的实体，也是反序列化了两次，第一次反序列化成DTObject获取全类名，第二次根据全类名和类型去反序列化真实的实体类。

 - 最后是在`EchoHandler`的`channelActive`方法中去测试发生数据：
 

```java
public class EchoHandler_2_0 extends ChannelInboundHandlerAdapter {
    //连接成功后发送消息测试
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        User user = new User();
        user.setBirthday(new Date());
        user.setUID(UUID.randomUUID().toString());
        user.setName("冉鹏峰");
        user.setAge(24);
        HashMap<String, User> map = new HashMap<>();
        map.put("数据一",user);
        map.put("数据2",user);
        map.put("数据3",user);
        ArrayList<User> list = new ArrayList<>();
        list.add(user);
        list.add(user);
        list.add(user);
        list.add(user);
        TcpProtocol_2_0 tcpProtocol= ProtocolUtils_2_0.prtclInstance(list,user.getClass().getName());
        ctx.write(tcpProtocol);
        ctx.flush();
    }
}
```
ProtocolUtils工具类是用来快速获取tcpProtocol对象的，具体代码：

```java
public class ProtocolUtils_2_0 {
    public final static byte OBJ_TYPE=0x51;
    public final static byte LIST_TYPE=0x52;
    public final static byte MAP_TYPE=0x53;
    /**
     * 创建集合类list map对象
     * */
    public static TcpProtocol_2_0 prtclInstance(Object o, String className){
        TcpProtocol_2_0 protocol = new TcpProtocol_2_0();
        if (o instanceof List){
            protocol.setType(LIST_TYPE);
        }else if (o instanceof Map){
            protocol.setType(MAP_TYPE);
        }else if (o instanceof Object){
            protocol.setType(OBJ_TYPE);
        }
        initProtocol(o, className, protocol);

        return protocol;
    }
    /***
     *
     * 创建单一的对象
     */
    public static TcpProtocol_2_0 prtclInstance(Object o){
        TcpProtocol_2_0 protocol = new TcpProtocol_2_0();
        protocol.setType(OBJ_TYPE);
        initProtocol(o, o.getClass().getName(), protocol);

        return protocol;
    }

    private static void initProtocol(Object o, String className, TcpProtocol_2_0 protocol) {
        try {
            DTObject dtObject = new DTObject();
            byte [] objectBytes= ByteUtils.InstanceObjectMapper().writeValueAsBytes(o);
            dtObject.setObject(objectBytes);
            dtObject.setClassName(className);
            byte[] bytes = ByteUtils.InstanceObjectMapper().writeValueAsBytes(dtObject);
            protocol.setLen(bytes.length);
            protocol.setData(bytes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
```
最后是运行结果测试：

 - 发送List对象时：
 
```
2019-01-15 18:21:10 DEBUG [org.wisdom.server.decoder.DecoderHandler_2_0] 开始解码数据……
2019-01-15 18:21:10 DEBUG [org.wisdom.server.decoder.DecoderHandler_2_0] 数据开头格式正确
2019-01-15 18:21:10 DEBUG [org.wisdom.server.decoder.DecoderHandler_2_0] 数据解码成功
2019-01-15 18:21:10 DEBUG [org.wisdom.server.decoder.DecoderHandler_2_0] 开始封装数据……
2019-01-15 18:21:10 INFO [org.wisdom.server.business.BusinessHandler_2_0] 这是一个List:[User{name='冉鹏峰', age=24, UID='2f7600e2-4714-4625-ad24-99947f182b76', birthday=Tue Jan 15 06:21:00 CST 2019}, User{name='冉鹏峰', age=24, UID='2f7600e2-4714-4625-ad24-99947f182b76', birthday=Tue Jan 15 06:21:00 CST 2019}, User{name='冉鹏峰', age=24, UID='2f7600e2-4714-4625-ad24-99947f182b76', birthday=Tue Jan 15 06:21:00 CST 2019}, User{name='冉鹏峰', age=24, UID='2f7600e2-4714-4625-ad24-99947f182b76', birthday=Tue Jan 15 06:21:00 CST 2019}]
```
List中的User泛型也成功的解码出来
 - 发生Map对象时
 

```
2019-01-15 18:22:49 DEBUG [org.wisdom.server.decoder.DecoderHandler_2_0] 开始解码数据……
2019-01-15 18:22:49 DEBUG [org.wisdom.server.decoder.DecoderHandler_2_0] 数据开头格式正确
2019-01-15 18:22:49 DEBUG [org.wisdom.server.decoder.DecoderHandler_2_0] 数据解码成功
2019-01-15 18:22:49 DEBUG [org.wisdom.server.decoder.DecoderHandler_2_0] 开始封装数据……
2019-01-15 18:22:49 INFO [org.wisdom.server.business.BusinessHandler_2_0] 这是一个Map:{数据一=User{name='冉鹏峰', age=24, UID='b975ac02-ae29-4190-866a-bdf19d373924', birthday=Tue Jan 15 06:22:00 CST 2019}, 数据2=User{name='冉鹏峰', age=24, UID='b975ac02-ae29-4190-866a-bdf19d373924', birthday=Tue Jan 15 06:22:00 CST 2019}, 数据3=User{name='冉鹏峰', age=24, UID='b975ac02-ae29-4190-866a-bdf19d373924', birthday=Tue Jan 15 06:22:00 CST 2019}}
```
Map中的泛型User也被成功解码并识别出来了

> 这里通过测试说明是能够处理传输集合类的信息的，实体类当然不在话下。但是通过一套流程下来：在客户端那里序列化了两次，在服务端反序列化了两次。序列化和反序列都是十分消耗性能的操作，按理说只序列化一次才是正常操作：对象实体--序列化---->字节数组 ；解码时候：字节数组----反序列化---->对象实体。下面是只序列化一次的方案2。
 - 方案2
 为了数据接收端能够动态的反序列化对象，因此把实体对象的class信息也一并传输过去，并将对象字节组和className放到`DTObject`这个实体做为数据的二次载体。
 

```java
public class DTObject {
    private String className;
    private byte[] object;
}
```
这样导致发送端和接收端都会为了`DTObject`而额外多做一次解析。**如果目的是为了简化协议结构则用方案一比较合适，如果考虑性更多性能上的问题，下面这种方式可能会更好。**

 1. 重新设计传输协议
 分析：在传入泛型对象时候，由于反编译需要同时声明泛型class和实体class，因此在协议中需要将这个泛型的类型type、实体的全类名className传入到接收端。可以在两边约定一种键值对类型的类型参照表：
 

```
|泛型类型|	代替数字	| 
|map 	|	0x51	|
|list	|	0x52	|
|单实体	|	0x53	|
```
甚至将实体的类型也对应的使用上面的方式：数字（key），全类名（value）在两端约定好。但是由于实体多样特效，可能需要将这些配置信息保存到一个激活的map中去，去而避免复杂的if else判断写法。随着实体类型的增加被激活的map的体积也要不断增加。
所以，简单处理：直接将className也包含到传输的数据中去。最终协议如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190218110227248.png)
对应的`TcpProtocol_3_0`代码：

```java
public class TcpProtocol_3_0 {
    private final byte header=0x58;
    private byte type;
    private byte classLen;
    private int len;
    private byte[] className;
    private byte [] data;
    private final byte tail=0x63;
 }
```
将协议实体组装工具`ProtocolUtils`稍作修改：

```java
public class ProtocolUtils {
    public final static byte OBJ_TYPE=0x51;
    public final static byte LIST_TYPE=0x52;
    public final static byte MAP_TYPE=0x53;
    /**
     * 创建集合类list map对象
     * */
    public static TcpProtocol_3_0 prtclInstance(Object o, String className){
        TcpProtocol_3_0 protocol = new TcpProtocol_3_0();
        if (o instanceof List){
            protocol.setType(LIST_TYPE);
        }else if (o instanceof Map){
            protocol.setType(MAP_TYPE);
        }else if (o instanceof Object){
            protocol.setType(OBJ_TYPE);
        }
        initProtocol(o, className, protocol);

        return protocol;
    }
    /***
     *
     * 创建单一的对象
     */
    public static TcpProtocol_3_0 prtclInstance(Object o){
        TcpProtocol_3_0 protocol = new TcpProtocol_3_0();
        protocol.setType(OBJ_TYPE);
        initProtocol(o, o.getClass().getName(), protocol);

        return protocol;
    }
    private static void initProtocol(Object o, String className, TcpProtocol_3_0 protocol) {
        byte [] classBytes=className.getBytes();
        try {
            byte [] objectBytes= ByteUtils.InstanceObjectMapper().writeValueAsBytes(o);
            protocol.setClassLen((byte) classBytes.length);
            protocol.setLen(objectBytes.length);
            protocol.setData(objectBytes);
            protocol.setClassName(classBytes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
```

 2. 编码器
 

```java
public class EncoderHandler_3_0 extends MessageToByteEncoder {
    private  Logger logger = Logger.getLogger(this.getClass());
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof TcpProtocol_3_0){
            TcpProtocol_3_0 protocol = (TcpProtocol_3_0) msg;
            out.writeByte(protocol.getHeader());
            out.writeByte(protocol.getType());
            out.writeByte(protocol.getClassLen());
            out.writeInt(protocol.getLen());
            out.writeBytes(protocol.getClassName());
            out.writeBytes(protocol.getData());
            out.writeByte(protocol.getTail());
            logger.debug("数据编码成功："+out);
        }else {
            logger.info("不支持的数据协议："+msg.getClass()+"\t期待的数据协议类是："+ TcpProtocol_3_0.class);
        }
    }
}
```

 3. 解码器 		
 解码器的设计逻辑，仍然按照设计的协议来：

 	1.解析并验证协议开头标志位`0x58`
 	2.解析出泛型type
 	3.解析出类名长度len1和数据组长度len2
 	4.根据剩余可读位数和len1+len2+1大小处理粘包/拆包
 	5.读取出类名className
 	6.读取实际的数据字节组data
 	7.解析并验证结束标志位
 	8.根据泛型type和className去反编译出data，获得传输的实际java实体
 	
 解码器代码：
 

```java
public class DecoderHandler_3_0 extends ByteToMessageDecoder {
    //最小的数据长度：开头标准位1字节
    private static int MIN_DATA_LEN=6+1+1+1;
    //数据解码协议的开始标志
    private static byte PROTOCOL_HEADER=0x58;
    //数据解码协议的结束标志
    private static byte PROTOCOL_TAIL=0x63;
    private Logger logger = Logger.getLogger(this.getClass());
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes()>MIN_DATA_LEN){
            logger.debug("开始解码数据……");
            //标记读操作的指针
            in.markReaderIndex();
            byte header=in.readByte();
            if (header==PROTOCOL_HEADER){
                logger.debug("数据开头格式正确");
                //读取字节数据的长度
                byte type=in.readByte();
                    int typeLen=in.readByte()&255;
                    int dataLen=in.readInt();
                    if (typeLen+dataLen<in.readableBytes()){
                        byte [] fullClassName=new byte[typeLen];
                        byte [] data=new byte[dataLen];
                        in.readBytes(fullClassName);
                        in.readBytes(data);
                        byte tail=in.readByte();
                        try {
                            Class<?> Type = Class.forName(new String(fullClassName));
                            if (tail==PROTOCOL_TAIL){
                                logger.debug("数据解码成功");
                                logger.debug("开始封装数据……");
                                ObjectMapper objectMapper = ByteUtils.InstanceObjectMapper();
                                if (type==ProtocolUtils.OBJ_TYPE){
                                    Object o = objectMapper.readValue(data, Type);
                                    out.add(o);
                                }else if (type==ProtocolUtils.MAP_TYPE){
                                    JavaType javaType= TypeFactory.defaultInstance().constructMapType(Map.class,String.class,Type);
                                    Object o = objectMapper.readValue(data, javaType);
                                    out.add(o);
                                }else if (type==ProtocolUtils.LIST_TYPE){
                                    JavaType javaType=TypeFactory.defaultInstance().constructCollectionType(List.class,Type);
                                    Object o = objectMapper.readValue(data, javaType);
                                    out.add(o);
                                }
                                //如果out有值，且in仍然可读，将继续调用decode方法再次解码in中的内容，以此解决粘包问题
                            }else {
                                logger.debug(String.format("数据解码协议结束标志位:%1$d [错误!]，期待的结束标志位是：%2$d",tail,PROTOCOL_TAIL));
                                return;
                            }
                        }catch (ClassNotFoundException e){
                            logger.error(String.format("反序列化对象的类找不到，期待的全类名是：%1$s,注意包名匹配！ ",fullClassName));
                            return;
                        }catch (Exception e){
                            logger.error(e);
                            return;
                        }

                    }else{
                        logger.debug(String.format("数据长度不够，数据协议len长度为：%1$d,数据包实际可读内容为：%2$d正在等待处理拆包……",dataLen+typeLen,in.readableBytes()));
                        in.resetReaderIndex();
                        /*
                         **结束解码，这种情况说明数据没有到齐，在父类ByteToMessageDecoder的callDecode中会对out和in进行判断
                         * 如果in里面还有可读内容即in.isReadable位true,cumulation中的内容会进行保留，，直到下一次数据到来，将两帧的数据合并起来，再解码。
                         * 以此解决拆包问题
                         */
                        return;
                    }
            }else {
                logger.debug("开头不对，可能不是期待的客服端发送的数，将自动略过这一个字节");
            }
        }else {
            logger.debug("数据长度不符合要求，期待最小长度是："+MIN_DATA_LEN+" 字节");
            return;
        }

    }
}
```

 4. 业务处理类的channelRead
 

```java
public class BusinessHandler_3_0 extends ChannelInboundHandlerAdapter {
    private ObjectMapper objectMapper= ByteUtils.InstanceObjectMapper();
    private Logger logger = Logger.getLogger(this.getClass());
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof List){
            logger.info("这是一个List:"+(List)msg);
        }else if (msg instanceof Map){
            logger.info("这是一个Map:"+(Map)msg);
        }else{
            logger.info("这是一个对象："+msg.getClass().getName());
            logger.info("这是一个对象："+msg);
        }
    }
}
```

 5. 客户端发送消息的处理器
 

```java
public class EchoHandler_3_0 extends ChannelInboundHandlerAdapter {
    //连接成功后发送消息测试
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
         User user = new User();
        user.setBirthday(new Date());
        user.setUID(UUID.randomUUID().toString());
        user.setName("冉鹏峰");
        user.setAge(24);
        Map<String,User> map=new HashMap<>();
        map.put("数据一",user);
        List<User> users=new ArrayList<>();
        users.add(user);
        TcpProtocol_3_0 protocol = ProtocolUtils.prtclInstance(map,user.getClass().getName());
        //传map
        ctx.write(protocol);//由于设置了编码器，这里直接传入自定义的对象
        ctx.flush();
        //传list
        ctx.write(ProtocolUtils.prtclInstance(users,user.getClass().getName()));
        ctx.flush();
        //传单一实体
        ctx.write(ProtocolUtils.prtclInstance(user));
        ctx.flush();
    }
}
```

 6. 测试运行结果
 客户端运行情况：
 

```
2019-02-18 11:25:22 DEBUG [org.wisdom.server.encoder.EncoderHandler_3_0] 数据编码成功：PooledUnsafeDirectByteBuf(ridx: 0, widx: 138, cap: 256)

2019-02-18 11:25:22 DEBUG [DEBUG] [id: 0x5b246c7d, L:/127.0.0.1:63155 - R:/127.0.0.1:8777] FLUSH
2019-02-18 11:25:22 DEBUG [org.wisdom.server.encoder.EncoderHandler_3_0] 数据编码成功：PooledUnsafeDirectByteBuf(ridx: 0, widx: 126, cap: 256)

2019-02-18 11:25:22 DEBUG [DEBUG] [id: 0x5b246c7d, L:/127.0.0.1:63155 - R:/127.0.0.1:8777] FLUSH
2019-02-18 11:25:22 DEBUG [org.wisdom.server.encoder.EncoderHandler_3_0] 数据编码成功：PooledUnsafeDirectByteBuf(ridx: 0, widx: 124, cap: 256)

2019-02-18 11:25:22 DEBUG [DEBUG] [id: 0x5b246c7d, L:/127.0.0.1:63155 - R:/127.0.0.1:8777] FLUSH
```
服务端接收到的运行情况：

```
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 开始解码数据……
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 数据开头格式正确
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 数据解码成功
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 开始封装数据……
2019-02-18 11:25:22 INFO [org.wisdom.server.business.BusinessHandler_3_0] 这是一个Map:{数据一=User{name='冉鹏峰', age=24, UID='3bb87b9f-a89c-4968-beec-a7b2a3b912b4', birthday=Mon Feb 18 11:25:00 CST 2019}}

2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 开始解码数据……
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 数据开头格式正确
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 数据解码成功
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 开始封装数据……
2019-02-18 11:25:22 INFO [org.wisdom.server.business.BusinessHandler_3_0] 这是一个List:[User{name='冉鹏峰', age=24, UID='3bb87b9f-a89c-4968-beec-a7b2a3b912b4', birthday=Mon Feb 18 11:25:00 CST 2019}]

2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 开始解码数据……
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 数据开头格式正确
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 数据解码成功
2019-02-18 11:25:22 DEBUG [org.wisdom.server.decoder.DecoderHandler_3_0] 开始封装数据……
2019-02-18 11:25:22 INFO [org.wisdom.server.business.BusinessHandler_3_0] 这是一个对象：pojo.User
2019-02-18 11:25:22 INFO [org.wisdom.server.business.BusinessHandler_3_0] 这是一个对象：User{name='冉鹏峰', age=24, UID='3bb87b9f-a89c-4968-beec-a7b2a3b912b4', birthday=Mon Feb 18 11:25:00 CST 2019}
```
**运行结果正常，可以混合传入单实体、泛型**

> 代码下载地址：https://github.com/Siwash/netty_TCP

 

 	

 
