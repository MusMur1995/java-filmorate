package ru.yandex.practicum.filmorate.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public User create(User user) {
        if (StringUtils.isBlank(user.getName())) {
            user.setName(user.getLogin());
        }
        return userStorage.create(user);
    }

    private void checkUserExists(Integer userId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
    }

    public User update(User newUser) {
        if (newUser.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }
        checkUserExists(newUser.getId());
        return userStorage.update(newUser);
    }

    public Collection<User> getAllUsers() {
        return userStorage.findAll();
    }


    public void addFriend(Integer userId, Integer friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить себя в друзья");
        }
        checkUserExists(userId);
        checkUserExists(friendId);
        userStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Integer userId, Integer friendId) {
        checkUserExists(userId);
        checkUserExists(friendId);
        userStorage.removeFriend(userId, friendId);
    }

    public List<User> getFriends(Integer userId) {
        checkUserExists(userId);
        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Integer id, Integer otherId) {
        checkUserExists(id);
        checkUserExists(otherId);
        return userStorage.getCommonFriends(id, otherId);
    }
}