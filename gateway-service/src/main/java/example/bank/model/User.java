package example.bank.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Transient;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("\"user\"")
public class User {

    @Id
    private Long id;

    @NotBlank(message = "Логин обязателен")
    private String username; // логин (используется также в Keycloak)

    @NotBlank(message = "Имя обязательно")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    private String lastName;

    @Email(message = "Некорректный формат email")
    private String email;

    @NotNull(message = "Дата рождения обязательна")
    private LocalDate birthDate;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
