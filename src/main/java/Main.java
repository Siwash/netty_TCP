import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wisdom.utils.ByteUtils;
import pojo.User;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException {
        User user = new User();
        user.setAge(20);
        user.setName("mrfox");
        user.setUID(UUID.randomUUID().toString());
        user.setBirthday(new Date());
        System.out.println(User.class.getName());
        Class type=Class.forName(User.class.getName());
        ObjectMapper objectMapper = ByteUtils.InstanceObjectMapper();
        try {
            String value = objectMapper.writeValueAsString(user);
            System.out.println(value);
            System.out.println(objectMapper.readValue(value.getBytes(),type));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
