package yes.example.no.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import yes.example.no.entity.Account;
import yes.example.no.entity.Group;
import yes.example.no.entity.GroupSymbol;
import yes.example.no.entity.Symbol;

public class SharedDTOs{
            public static class AccountDto {
        private Long id;
        private String username;
        private double balance;
        private double equity;
        private double marginUsed;
        private double freeMargin;
        private boolean admin;
        private String groupName;
        
        public AccountDto(Account account) {
            this.id = account.getId();
            this.username = account.getUsername();
            this.balance = account.getBalance();
            this.equity = account.getEquity();
            this.marginUsed = account.getMarginUsed();
            this.freeMargin = account.getFreeMargin();
            this.admin = account.isAdmin();
            this.groupName = account.getGroup() != null ? account.getGroup().getName() : null;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
        public double getEquity() { return equity; }
        public void setEquity(double equity) { this.equity = equity; }
        public double getMarginUsed() { return marginUsed; }
        public void setMarginUsed(double marginUsed) { this.marginUsed = marginUsed; }
        public double getFreeMargin() { return freeMargin; }
        public void setFreeMargin(double freeMargin) { this.freeMargin = freeMargin; }
        public boolean isAdmin() { return admin; }
        public void setAdmin(boolean admin) { this.admin = admin; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SymbolDto {
        private Long id;
        private String name;
        private double bidPrice;
        private double askPrice;
        private boolean active;
        
        public SymbolDto(Symbol symbol) {
            this.id = symbol.getId();
            this.name = symbol.getName();
            this.bidPrice = symbol.getBidPrice();
            this.askPrice = symbol.getAskPrice();
            this.active = symbol.isActive();
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getBidPrice() { return bidPrice; }
        public void setBidPrice(double bidPrice) { this.bidPrice = bidPrice; }
        public double getAskPrice() { return askPrice; }
        public void setAskPrice(double askPrice) { this.askPrice = askPrice; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

        public static class GroupDetailDto {
        private Long id;
        private String name;
        private String description;
        private boolean active;
        private int memberCount;
        private int symbolCount;
        
        public GroupDetailDto(Group group, int memberCount, int symbolCount) {
            this.id = group.getId();
            this.name = group.getName();
            this.description = group.getDescription();
            this.active = group.isActive();
            this.memberCount = memberCount;
            this.symbolCount = symbolCount;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
        public int getSymbolCount() { return symbolCount; }
        public void setSymbolCount(int symbolCount) { this.symbolCount = symbolCount; }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupSymbolUpdateRequest {
        private boolean canViewQuotes;
        private boolean canTrade;
        private double bidMarkup;
        private double askMarkup;
        private double bidMarkupPercent;
        private double askMarkupPercent;
        
        // Getters and setters
        public boolean isCanViewQuotes() { return canViewQuotes; }
        public void setCanViewQuotes(boolean canViewQuotes) { this.canViewQuotes = canViewQuotes; }
        public boolean isCanTrade() { return canTrade; }
        public void setCanTrade(boolean canTrade) { this.canTrade = canTrade; }
        public double getBidMarkup() { return bidMarkup; }
        public void setBidMarkup(double bidMarkup) { this.bidMarkup = bidMarkup; }
        public double getAskMarkup() { return askMarkup; }
        public void setAskMarkup(double askMarkup) { this.askMarkup = askMarkup; }
        public double getBidMarkupPercent() { return bidMarkupPercent; }
        public void setBidMarkupPercent(double bidMarkupPercent) { this.bidMarkupPercent = bidMarkupPercent; }
        public double getAskMarkupPercent() { return askMarkupPercent; }
        public void setAskMarkupPercent(double askMarkupPercent) { this.askMarkupPercent = askMarkupPercent; }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupSymbolDto {
    private Long id;
    private SymbolDto symbol;
    private boolean canViewQuotes;
    private boolean canTrade;
    private double bidMarkup;
    private double askMarkup;
    private double bidMarkupPercent;
    private double askMarkupPercent;

    // Add this constructor that was missing:
    public GroupSymbolDto(GroupSymbol groupSymbol) {
        this.id = groupSymbol.getId();
        this.symbol = new SymbolDto(groupSymbol.getSymbol());
        this.canViewQuotes = groupSymbol.isCanViewQuotes();
        this.canTrade = groupSymbol.isCanTrade();
        this.bidMarkup = groupSymbol.getBidMarkup();
        this.askMarkup = groupSymbol.getAskMarkup();
        this.bidMarkupPercent = groupSymbol.getBidMarkupPercent();
        this.askMarkupPercent = groupSymbol.getAskMarkupPercent();
    }
}

    public static class CreateGroupRequest {
        private String name;
        private String description;
        private boolean active = true;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class GroupSymbolConfigDto {
        private Long symbolId;
        private boolean canViewQuotes = true;
        private boolean canTrade = true;
        private double bidMarkup = 0.0;
        private double askMarkup = 0.0;
        private double bidMarkupPercent = 0.0;
        private double askMarkupPercent = 0.0;
        
        // Getters and setters
        public Long getSymbolId() { return symbolId; }
        public void setSymbolId(Long symbolId) { this.symbolId = symbolId; }
        public boolean isCanViewQuotes() { return canViewQuotes; }
        public void setCanViewQuotes(boolean canViewQuotes) { this.canViewQuotes = canViewQuotes; }
        public boolean isCanTrade() { return canTrade; }
        public void setCanTrade(boolean canTrade) { this.canTrade = canTrade; }
        public double getBidMarkup() { return bidMarkup; }
        public void setBidMarkup(double bidMarkup) { this.bidMarkup = bidMarkup; }
        public double getAskMarkup() { return askMarkup; }
        public void setAskMarkup(double askMarkup) { this.askMarkup = askMarkup; }
        public double getBidMarkupPercent() { return bidMarkupPercent; }
        public void setBidMarkupPercent(double bidMarkupPercent) { this.bidMarkupPercent = bidMarkupPercent; }
        public double getAskMarkupPercent() { return askMarkupPercent; }
        public void setAskMarkupPercent(double askMarkupPercent) { this.askMarkupPercent = askMarkupPercent; }
    }

}