package yes.example.no.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import yes.example.no.entity.Account;
import yes.example.no.entity.Group;
import yes.example.no.repository.AccountRepository;
import yes.example.no.repository.GroupRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private final AccountRepository accountRepository;
    private final GroupRepository groupRepository;

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

    public void notifyUserWatchlistChanged(String username) {
        Map<String, Object> notification = Map.of(
            "type", "WATCHLIST_CHANGED",
            "message", "Your watchlist has been updated",
            "timestamp", System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
    }

    public void notifyGroupMembersSymbolsChanged(Long groupId, String message) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isPresent()) {
            Group group = groupOpt.get();
            List<Account> groupMembers = accountRepository.findByGroup(group);
            
            Map<String, Object> notification = Map.of(
                "type", "GROUP_SYMBOLS_CHANGED",
                "groupId", groupId,
                "groupName", group.getName(),
                "message", message,
                "timestamp", System.currentTimeMillis()
            );
            
            // Notify all group members to refresh their watchlist
            groupMembers.forEach(member -> {
                messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/notifications", notification);
            });
        }
    }

        public void notifyGroupSymbolChanged(Long groupId, Long groupSymbolId) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isPresent()) {
            Group group = groupOpt.get();
            List<Account> groupMembers = accountRepository.findByGroup(group);
            List<String> usernames = groupMembers.stream()
                    .map(Account::getUsername)
                    .collect(Collectors.toList());
            
            Map<String, Object> notification = Map.of(
                "type", "GROUP_SYMBOL_CHANGED",
                "groupId", groupId,
                "groupSymbolId", groupSymbolId,
                "groupName", group.getName(),
                "message", "Symbol access in your group has been updated",
                "timestamp", System.currentTimeMillis()
            );
            
            // Notify all group members
            usernames.forEach(username -> {
                messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
            });
            
            // Also notify admins
            messagingTemplate.convertAndSend("/topic/group-updates", notification);
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

    public void notifyGroupPermissionsUpdate(Long groupId, List<Map<String, Object>> permissions) {
        Map<String, Object> payload = Map.of(
            "type", "PERMISSIONS_UPDATE",
            "groupId", groupId,
            "symbols", permissions,
            "timestamp", System.currentTimeMillis()
        );
    
        // send to all clients in that group
        messagingTemplate.convertAndSend("/topic/group-permissions." + groupId, payload);
    }

}

