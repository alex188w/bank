package example.bank.service;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import example.bank.repository.UserRepository;

import example.bank.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Mono<User> registerUser(User user) {
        if (user.getBirthDate().plusYears(18).isAfter(LocalDate.now())) {
            return Mono.error(new IllegalArgumentException("Возраст должен быть старше 18 лет"));
        }

        // Проверяем, есть ли пользователь с таким username в БД
        return userRepository.findByUsername(user.getUsername())
                .flatMap(existing -> Mono.<User>error(
                        new IllegalArgumentException("Пользователь с таким логином уже существует")))
                .switchIfEmpty(Mono.defer(() -> keycloakService.userExistsInKeycloak(user.getUsername())
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new IllegalArgumentException(
                                        "Такой логин уже зарегистрирован в Keycloak"));
                            }
                            // Хэшируем пароль
                            String rawPassword = user.getPassword(); // с формы
                            String hashedPassword = passwordEncoder.encode(rawPassword);
                            user.setPassword(hashedPassword);

                            // Создаём пользователя в Keycloak и сохраняем в БД
                            return keycloakService.createUserInKeycloak(
                                    user.getUsername(),
                                    user.getEmail(),
                                    user.getPassword())
                                    .then(userRepository.save(user));
                        })));

    }

    // Получение профиля пользователя
    public Mono<User> getProfile(String username) {
        return userRepository.findByUsername(username);
    }

    // Обновление профиля пользователя
    public Mono<User> updateProfile(String username, User updatedUser) {
        return userRepository.findByUsername(username)
                .flatMap(user -> {
                    // Валидация
                    if (updatedUser.getFirstName() == null || updatedUser.getLastName() == null ||
                            updatedUser.getEmail() == null || updatedUser.getBirthDate() == null) {
                        return Mono.error(new IllegalArgumentException("Не все поля заполнены"));
                    }
                    if (Period.between(updatedUser.getBirthDate(), LocalDate.now()).getYears() < 18) {
                        return Mono.error(new IllegalArgumentException("Возраст меньше 18 лет"));
                    }

                    // Обновляем данные
                    user.setFirstName(updatedUser.getFirstName());
                    user.setLastName(updatedUser.getLastName());
                    user.setEmail(updatedUser.getEmail());
                    user.setBirthDate(updatedUser.getBirthDate());

                    return userRepository.save(user)
                            .doOnSuccess(u -> log.info("Профиль обновлён: {}", u));
                });
    }
}
