package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Integer, User> users = new HashMap<>();
    private static final String BIRTHDAY_IN_FUTURE_MSG = "Дата рождения не может быть в будущем";
    private static final String LOGIN_INVALID_MSG = "Логин не может быть пустым или содержать пробелы";
    private static final String EMAIL_INVALID_MSG = "Электронная почта не может быть пустой и должна содержать символ @";

    @PostMapping
    public User create(@RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user.getEmail());

        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Попытка создания пользователя с некорректным email: {}", user.getEmail());
            throw new ValidationException(EMAIL_INVALID_MSG);
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Попытка создания пользователя с некорректным логином: {}", user.getLogin());
            throw new ValidationException(LOGIN_INVALID_MSG);
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Для пользователя {} установлено имя из логина: {}", user.getEmail(), user.getLogin());
        }

        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Попытка создания пользователя с некорректной датой рождения: {}", user.getBirthday());
            throw new ValidationException(BIRTHDAY_IN_FUTURE_MSG);
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Пользователь создан успешно: {} (ID: {})", user.getEmail(), user.getId());
        return user;
    }

    // вспомогательный метод для генерации идентификатора нового user
    private int getNextId() {
        int currentMaxId = users.keySet()
                .stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @PutMapping
    public User updateUser(@RequestBody User newUser) {
        log.info("Получен запрос на обновление пользователя с ID: {}", newUser.getId());

        if (newUser.getId() == null) {
            log.warn("Попытка обновления пользователя без указания ID");
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(newUser.getId())) {
            log.warn("Попытка обновления несуществующего пользователя с ID: {}", newUser.getId());
            throw new ValidationException("Пользователь с id = " + newUser.getId() + " не найден");
        }

        // Проверка уникальности email
        if (newUser.getEmail() != null) {
            boolean emailExists = users.values().stream()
                    .anyMatch(existingUser ->
                            existingUser.getEmail().equals(newUser.getEmail()) &&
                                    !existingUser.getId().equals(newUser.getId())
                    );

            if (emailExists) {
                log.warn("Попытка обновления email на уже существующий: {} (ID: {})",
                        newUser.getEmail(), newUser.getId());
                throw new ValidationException("Этот email уже используется");
            }
        }

        User oldUser = users.get(newUser.getId());

        // Обновление полей с валидацией
        if (newUser.getEmail() != null) {
            if (newUser.getEmail().isBlank() || !newUser.getEmail().contains("@")) {
                log.warn("Попытка обновления email на некорректный: {} (ID: {})",
                        newUser.getEmail(), newUser.getId());
                throw new ValidationException(EMAIL_INVALID_MSG);
            }
            oldUser.setEmail(newUser.getEmail().trim());
            log.debug("Обновлен email пользователя ID {}: {}", newUser.getId(), newUser.getEmail());
        }

        if (newUser.getLogin() != null) {
            if (newUser.getLogin().trim().isEmpty() || newUser.getLogin().contains(" ")) {
                log.warn("Попытка обновления логина на некорректный: {} (ID: {})",
                        newUser.getLogin(), newUser.getId());
                throw new ValidationException(LOGIN_INVALID_MSG);
            }
            oldUser.setLogin(newUser.getLogin());
            log.debug("Обновлен логин пользователя ID {}: {}", newUser.getId(), newUser.getLogin());
        }

        if (newUser.getName() != null) {
            oldUser.setName(newUser.getName().trim());
            log.debug("Обновлено имя пользователя ID {}: {}", newUser.getId(), newUser.getName());
        }

        if (newUser.getBirthday() != null) {
            if (newUser.getBirthday().isAfter(LocalDate.now())) {
                log.warn("Попытка обновления даты рождения на будущую: {} (ID: {})",
                        newUser.getBirthday(), newUser.getId());
                throw new ValidationException(BIRTHDAY_IN_FUTURE_MSG);
            }
            oldUser.setBirthday(newUser.getBirthday());
            log.debug("Обновлена дата рождения пользователя ID {}: {}", newUser.getId(), newUser.getBirthday());
        }

        log.info("Пользователь с ID {} успешно обновлен", newUser.getId());
        return oldUser;
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Количество пользователей: {}", users.size());
        return users.values();
    }
}