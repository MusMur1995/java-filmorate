package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.user.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    @Override
    public User create(User user) {
        log.info("Создание пользователя: {}", user.getEmail());

        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        Integer id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        user.setId(id);

        log.info("Пользователь создан с ID: {}", id);
        return user;
    }

    @Override
    public User update(User newUser) {
        log.info("Обновление пользователя с ID: {}", newUser.getId());

        if (newUser.getId() == null) {
            throw new ValidationException("ID пользователя не может быть null");
        }

        findById(newUser.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + newUser.getId() + " не найден"));

        String checkEmailSql = "SELECT COUNT(*) FROM users WHERE email = ? AND id != ?";
        Integer emailCount = jdbcTemplate.queryForObject(checkEmailSql, Integer.class, newUser.getEmail(), newUser.getId());

        if (emailCount != null && emailCount > 0) {
            throw new ValidationException("Этот email уже используется");
        }

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getName(),
                Date.valueOf(newUser.getBirthday()),
                newUser.getId());

        if (updated == 0) {
            throw new NotFoundException("Пользователь с ID " + newUser.getId() + " не найден");
        }

        return newUser;
    }

    @Override
    public Optional<User> findById(Integer id) {
        log.info("Поиск пользователя по ID: {}", id);

        String sql = "SELECT * FROM users WHERE id = ?";

        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            loadFriends(user);
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Пользователь с ID {} не найден", id);
            return Optional.empty();
        }
    }

    @Override
    public Collection<User> findAll() {
        log.info("Получение всех пользователей");

        String sql = "SELECT * FROM users ORDER BY id";

        List<User> users = jdbcTemplate.query(sql, userRowMapper);

        users.forEach(this::loadFriends);

        return users;
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        log.info("Добавление пользователя {} в друзья пользователю {}", friendId, userId);

        validateUserExists(userId);
        validateUserExists(friendId);

        String checkSql = "SELECT COUNT(*) FROM friendship WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count != null && count > 0) {
            throw new ValidationException("Пользователь уже добавлен в друзья");
        }

        String sql = "INSERT INTO friendship (user_id, friend_id) VALUES (?, ?)";

        try {
            jdbcTemplate.update(sql, userId, friendId);
            log.info("Друг добавлен");
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Ошибка при добавлении в друзья");
        }
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        log.info("Удаление пользователя {} из друзей пользователя {}", friendId, userId);

        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";

        int deleted = jdbcTemplate.update(sql, userId, friendId);

        if (deleted == 0) {
            log.warn("Дружба не найдена: пользователь {} -> {}", userId, friendId);
        }
    }

    @Override
    public List<User> getFriends(Integer userId) {
        log.info("Получение друзей пользователя {}", userId);

        validateUserExists(userId);

        String sql = "SELECT u.* FROM friendship f " +
                "JOIN users u ON f.friend_id = u.id " +
                "WHERE f.user_id = ? " +
                "ORDER BY u.id";

        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherUserId) {
        log.info("Получение общих друзей пользователей {} и {}", userId, otherUserId);

        validateUserExists(userId);
        validateUserExists(otherUserId);

        String sql = "SELECT u.* FROM users u " +
                "WHERE u.id IN (" +
                "    SELECT f1.friend_id FROM friendship f1 " +
                "    WHERE f1.user_id = ?" +
                ") AND u.id IN (" +
                "    SELECT f2.friend_id FROM friendship f2 " +
                "    WHERE f2.user_id = ?" +
                ") " +
                "ORDER BY u.id";

        return jdbcTemplate.query(sql, userRowMapper, userId, otherUserId);
    }

    private void loadFriends(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        String sql = "SELECT friend_id FROM friendship WHERE user_id = ?";

        List<Integer> friendIds = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getInt("friend_id"),
                user.getId());

        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        } else {
            user.getFriends().clear();
        }

        user.getFriends().addAll(friendIds);
    }

    private void validateUserExists(Integer userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);

        if (count == null || count == 0) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }
}