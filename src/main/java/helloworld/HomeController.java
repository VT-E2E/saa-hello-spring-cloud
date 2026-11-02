package helloworld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import io.pivotal.cfenv.core.CfApplication;
import io.pivotal.cfenv.core.CfEnv;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @Autowired(required = false) DataSource dataSource;
    @Autowired(required = false) RedisConnectionFactory redisConnectionFactory;
    @Autowired(required = false) MongoDbFactory mongoDbFactory;
    @Autowired(required = false) ConnectionFactory rabbitConnectionFactory;

    private final CfEnv cfEnv = new CfEnv();

    public static class InstanceInfo {
        private final String appId;
        private final String instanceId;
        private final Map<String, Object> properties;

        InstanceInfo(String appId, String instanceId, Map<String, Object> properties) {
            this.appId = appId;
            this.instanceId = instanceId;
            this.properties = properties;
        }

        public String getAppId() { return appId; }
        public String getInstanceId() { return instanceId; }
        public Map<String, Object> getProperties() { return properties; }
    }

    @RequestMapping("/")
    public String home(Model model) {
        InstanceInfo instanceInfo = null;
        if (cfEnv.isInCf()) {
            CfApplication app = cfEnv.getApp();
            instanceInfo = new InstanceInfo(app.getApplicationId(), app.getInstanceId(), app.getMap());
        }
        model.addAttribute("instanceInfo", instanceInfo);

        if (instanceInfo != null) {
            Map<Class<?>, String> services = new LinkedHashMap<Class<?>, String>();
            services.put(dataSource.getClass(), toString(dataSource));
            services.put(mongoDbFactory.getClass(), toString(mongoDbFactory));
            services.put(redisConnectionFactory.getClass(), toString(redisConnectionFactory));
            services.put(rabbitConnectionFactory.getClass(), toString(rabbitConnectionFactory));
            model.addAttribute("services", services.entrySet());
        }

        return "home";
    }

    private String toString(DataSource dataSource) {
        if (dataSource == null) {
            return "<none>";
        } else {
            try {
                Field urlField = ReflectionUtils.findField(dataSource.getClass(), "url");
                ReflectionUtils.makeAccessible(urlField);
                return stripCredentials((String) urlField.get(dataSource));
            } catch (Exception fe) {
                try {
                    Method urlMethod = ReflectionUtils.findMethod(dataSource.getClass(), "getUrl");
                    ReflectionUtils.makeAccessible(urlMethod);
                    return stripCredentials((String) urlMethod.invoke(dataSource, (Object[])null));
                } catch (Exception me){
                    return "<unknown> " + dataSource.getClass();
                }
            }
        }
    }

    private String toString(MongoDbFactory mongoDbFactory) {
        if (mongoDbFactory == null) {
            return "<none>";
        } else {
            try {
                return mongoDbFactory.getMongoDatabase().getName();
            } catch (Exception ex) {
                return "<invalid database> " + mongoDbFactory.getClass();
            }
        }
    }

    private String toString(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory == null) {
            return "<none>";
        } else {
            if (redisConnectionFactory instanceof JedisConnectionFactory jcf) {
                return jcf.getHostName().toString() + ":" + jcf.getPort();
            } else if (redisConnectionFactory instanceof LettuceConnectionFactory lcf) {
                return lcf.getHostName().toString() + ":" + lcf.getPort();
            }
            return "<unknown> " + redisConnectionFactory.getClass();
        }
    }

    private String toString(ConnectionFactory rabbitConnectionFactory) {
        if (rabbitConnectionFactory == null) {
            return "<none>";
        } else {
            return rabbitConnectionFactory.getHost() + ":"
                    + rabbitConnectionFactory.getPort();
        }
    }

    private String stripCredentials(String urlString) {
        try {
            if (urlString.startsWith("jdbc:")) {
                urlString = urlString.substring("jdbc:".length());
            }
            URI url = new URI(urlString);
            return new URI(url.getScheme(), null, url.getHost(), url.getPort(), url.getPath(), null, null).toString();
        }
        catch (URISyntaxException e) {
            System.out.println(e);
            return "<bad url> " + urlString;
        }
    }

}
