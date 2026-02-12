package ru.yandex.practicum.filmorate.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.user.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public User create(User user) {
        log.info("Создание пользователя: {}", user.getEmail());

        if (StringUtils.isBlank(user.getName())) {
            user.setName(user.getLogin());
            log.info("Имя пользователя установлено как логин: {}", user.getLogin());
        }

        return userStorage.create(user);
    }

    private User getUser(Integer userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
    }

    public User update(User newUser) {
        log.info("Обновление пользователя с ID: {}", newUser.getId());

        if (newUser.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        getUser(newUser.getId());
        return userStorage.update(newUser);
    }

    public Collection<User> getAllUsers() {
        log.info("Получение всех пользователей");
        return userStorage.findAll();
    }

    public void addFriend(Integer userId, Integer friendId) {
        log.info("Добавление пользователя {} в друзья пользователю {}", friendId, userId);

        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить себя в друзья");
        }

        getUser(userId);
        getUser(friendId);
        userStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Integer userId, Integer friendId) {
        log.info("Удаление пользователя {} из друзей пользователя {}", friendId, userId);

        getUser(userId);
        getUser(friendId);
        userStorage.removeFriend(userId, friendId);
    }

    public List<User> getFriends(Integer userId) {
        log.info("Получение друзей пользователя {}", userId);
        getUser(userId);
        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Integer id, Integer otherId) {
        log.info("Получение общих друзей пользователей {} и {}", id, otherId);
        getUser(id);
        getUser(otherId);
        return userStorage.getCommonFriends(id, otherId);
    }

    public User getUserById(Integer id) {
        log.info("Получение пользователя по ID: {}", id);
        return getUser(id);
    }
}