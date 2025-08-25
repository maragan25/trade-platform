package yes.example.no.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class WebSocketNotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void notifyPriceUpdate(Map<String, Object> priceUpdate) {
        messagingTemplate.convertAndSend("/topic/prices", priceUpdate);
    }
    
    public void notifyGroupChange(Long groupId, List<String> affectedUsers, String message) {
        Map<String, Object> notification = Map.of(
            "type", "GROUP_CHANGED",
            "groupId", groupId,
            "affectedUsers", affectedUsers,
            "message", message
        );
        
        // Send to all clients
        messagingTemplate.convertAndSend("/topic/group-updates", notification);
        
        // Send to specific affected users
        for (String username : affectedUsers) {
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
        }
    }
    
    public void notifySymbolAccessChange(Long groupId, Long symbolId, List<String> affectedUsers) {
        Map<String, Object> notification = Map.of(
            "type", "SYMBOL_ACCESS_CHANGED",
            "groupId", groupId,
            "symbolId", symbolId,
            "affectedUsers", affectedUsers,
            "message", "Your symbol access has been updated"
        );
        
        messagingTemplate.convertAndSend("/topic/group-updates", notification);
        
        for (String username : affectedUsers) {
            messagingTemplate.convertAndSendToUser(username, "/queue/symbol-refresh", notification);
        }
    }
    
    public void broadcastAdminMessage(String type, String message) {
        Map<String, Object> adminMessage = Map.of(
            "type", type,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSend("/topic/admin-messages", adminMessage);
    }
}

