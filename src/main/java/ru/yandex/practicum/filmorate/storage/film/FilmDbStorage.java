package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.film.Film;
import ru.yandex.practicum.filmorate.model.film.Genre;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component

@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;

    @Override
    public Film create(Film film) {
        log.info("Создание фильма: {}", film.getName());
        String sql = "INSERT INTO films (name, description, releaseDate, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        Integer id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        film.setId(id);

        saveGenres(film);
        log.info("Фильм создан с ID: {}", id);
        return film;
    }

    private void saveGenres(Film film) {

        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String insertSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

            film.getGenres().forEach(genre -> {
                jdbcTemplate.update(insertSql, film.getId(), genre.getId());
            });
        }
    }

    @Override
    public Film update(Film film) {
        log.info("Обновление фильма с ID: {}", film.getId());

        if (film.getId() == null) {
            throw new ValidationException("ID фильма не может быть null");
        }

        findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + film.getId() + " не найден"));

        String sql = "UPDATE films SET name = ?, description = ?, releaseDate = ?, duration = ?, mpa_id = ? " +
                "WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        if (updated == 0) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }

        saveGenres(film);

        return findById(film.getId()).orElse(film);
    }

    @Override
    public Collection<Film> findAll() {
        log.info("Получение всех фильмов");

        String sql = "SELECT f.*, m.mpa_name FROM films f " +
                "LEFT JOIN mpa_rating m ON f.mpa_id = m.id " +
                "ORDER BY f.id";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);

        films.forEach(film -> {
            loadGenres(film);
            loadLikes(film);
        });

        return films;
    }

    @Override
    public Optional<Film> findById(Integer id) {
        log.info("Поиск фильма по ID: {}", id);

        String sql = "SELECT f.*, m.mpa_name FROM films f " +
                "LEFT JOIN mpa_rating m ON f.mpa_id = m.id " +
                "WHERE f.id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);

            if (film != null) {
                loadGenres(film);
                loadLikes(film);
            }

            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private void loadGenres(Film film) {
        String sql = "SELECT g.* FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY g.id";

        List<Genre> genres = jdbcTemplate.query(sql,
                (rs, rowNum) -> Genre.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("genre_name"))
                        .build(),
                film.getId());

        film.getGenres().clear();
        film.getGenres().addAll(genres);
    }

    private void loadLikes(Film film) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";

        List<Integer> likes = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getInt("user_id"),
                film.getId());

        film.getLikes().clear();
        film.getLikes().addAll(likes);
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        log.info("Добавление лайка фильму ID: {} от пользователя ID: {}", filmId, userId);

        validateFilmExists(filmId);
        validateUserExists(userId);

        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";

        try {
            jdbcTemplate.update(sql, filmId, userId);
            log.info("Лайк успешно добавлен");
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Лайк уже существует");
        }
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        log.info("Удаление лайка фильму ID: {} от пользователя ID: {}", filmId, userId);

        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";

        int deleted = jdbcTemplate.update(sql, filmId, userId);

        if (deleted == 0) {
            throw new NotFoundException("Лайк не найден");
        }

        log.info("Лайк успешно удален");
    }

    @Override
    public Collection<Film> getPopularFilms(int count) {
        log.info("Получение {} популярных фильмов", count);

        if (count <= 0) {
            count = 10;
        }

        String sql = "SELECT f.*, m.mpa_name, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_rating m ON f.mpa_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "GROUP BY f.id, m.mpa_name " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);

        films.forEach(film -> {
            loadGenres(film);
            loadLikes(film);
        });

        return films;
    }

    private void validateFilmExists(Integer filmId) {
        String sql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId);

        if (count == null || count == 0) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
    }

    private void validateUserExists(Integer userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);

        if (count == null || count == 0) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }
}