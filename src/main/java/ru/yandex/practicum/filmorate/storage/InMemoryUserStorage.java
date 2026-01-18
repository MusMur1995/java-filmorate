package ru.yandex.practicum.filmorate.storage;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Integer, User> users = new HashMap<>();
    private final Map<Integer, Set<Integer>> friends = new HashMap<>();

    @Override
    public User create(User user) {
        if (StringUtils.isBlank(user.getName())) {
            user.setName(user.getLogin());
        }
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    private int getNextId() {
        int currentMaxId = users.keySet()
                .stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @Override
    public User update(User newUser) {
        if (newUser.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(newUser.getId())) {
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }

        boolean emailExists = users.values().stream()
                .anyMatch(existingUser ->
                        existingUser.getEmail().equals(newUser.getEmail()) &&
                                !existingUser.getId().equals(newUser.getId())
                );

        if (emailExists) {
            throw new ValidationException("Этот email уже используется");
        }

        users.put(newUser.getId(), newUser);
        return newUser;
    }

    @Override
    public Optional<User> findById(Integer id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        if (!users.containsKey(friendId)) {
            throw new NotFoundException("Пользователь с id = " + friendId + " не найден");
        }

        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить себя в друзья");
        }

        Set<Integer> userFriends = friends.computeIfAbsent(userId, k -> new HashSet<>());
        userFriends.add(friendId);

        Set<Integer> friendFriends = friends.computeIfAbsent(friendId, k -> new HashSet<>());
        friendFriends.add(userId);
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {

        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        if (!users.containsKey(friendId)) {
            throw new NotFoundException("Пользователь с id = " + friendId + " не найден");
        }

        Set<Integer> userFriends = friends.get(userId);

        if (userFriends == null || !userFriends.contains(friendId)) {
            return;
        }

        userFriends.remove(friendId);

        Set<Integer> friendFriends = friends.get(friendId);
        if (friendFriends != null) {
            friendFriends.remove(userId);
        }
    }

    @Override
    public List<User> getFriends(Integer userId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        Set<Integer> friendsIds = friends.get(userId);

        if (friendsIds == null || friendsIds.isEmpty()) {
            return Collections.emptyList();
        }

        return getUsers(friendsIds);
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherUserId) {
        Set<Integer> friends1 = friends.getOrDefault(userId, Collections.emptySet());
        Set<Integer> friends2 = friends.getOrDefault(otherUserId, Collections.emptySet());

        Set<Integer> commonIds = new HashSet<>(friends1);
        commonIds.retainAll(friends2);

        return getUsers(commonIds);
    }

    private List<User> getUsers(Set<Integer> users) {
        List<User> result = new ArrayList<>();
        for (Integer commonId : users) {
            User commonFriend = this.users.get(commonId);
            if (commonFriend != null) {
                result.add(commonFriend);
            }
        }
        return result;
    }
}
