package yes.example.no.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter
@Setter
@Table(name = "groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    private String description;
    private boolean active = true;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Prevent circular reference in JSON
    private Set<GroupSymbol> groupSymbols;
    
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @JsonIgnore // Prevent circular reference in JSON
    private Set<Account> accounts;
}
