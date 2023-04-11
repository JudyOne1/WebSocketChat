package com.itheima.ws;

import com.alibaba.fastjson.JSON;
import com.itheima.config.GetHttpSessionConfig;
import com.itheima.utils.MessageUtils;
import com.itheima.ws.pojo.Message;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Judy
 * @create 2023-04-09-21:03
 * 每一个客户端建立连接都会创建一个endpoint对象（多例的）
 */
@ServerEndpoint(value = "/chat",configurator = GetHttpSessionConfig.class)
@Component
public class ChatEndpoint {

    //所有的endpoint实例都用同一个map集合
    public static final Map<String,Session> onlineUser = new ConcurrentHashMap<>();

    private HttpSession httpSession;

    /**
     * 建立websocket连接后，被调用
     * @param session
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig){
        //1. 将session进行保存 用户名-session
        //赋值httpSession 取得用户名
        this.httpSession = (HttpSession) endpointConfig.getUserProperties().get(HttpSession.class.getName());
        onlineUser.put(httpSession.getAttribute("user").toString(),session);
        //2. 广播消息，推送到所有客户端
        String message = MessageUtils.getMessage(true, null, onlineUser.keySet());
        broadcastAllUser(message);
    }

    private void broadcastAllUser(String message){
        //遍历map
        Set<Map.Entry<String, Session>> entries = onlineUser.entrySet();
        for (Map.Entry<String, Session> entry : entries) {
            //获取所有用户对应的session对象
            Session session = entry.getValue();
            //发送消息(同步)
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 浏览器发送消息到服务端时被调用，
     * @param message
     */
    @OnMessage
    public void onMessage(String message) {
        //推送消息给指定用户
        Message msg = JSON.parseObject(message, Message.class);
        String toName = msg.getToName();
        String realMsg = msg.getMessage();
        Session session = onlineUser.get(toName);
        String str = MessageUtils.getMessage(false, httpSession.getAttribute("user").toString(), realMsg);
        try {
            session.getBasicRemote().sendText(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 断开websocket后，被调用
     * @param session
     */
    @OnClose
    public void onClose(Session session,EndpointConfig endpointConfig){
        //1. 移除map
        onlineUser.remove(httpSession.getAttribute("user").toString());
        //2. 广播
        String message = MessageUtils.getMessage(true, null, onlineUser.keySet());
        broadcastAllUser(message);

    }
}
