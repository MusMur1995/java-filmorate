package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.film.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<Mpa> findAll() {
        log.info("Получение всех рейтингов MPA");
        String sql = "SELECT * FROM mpa_rating ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToMpa);
    }

    @Override
    public Optional<Mpa> findById(Integer id) {
        log.info("Поиск рейтинга MPA по ID: {}", id);
        String sql = "SELECT * FROM mpa_rating WHERE id = ?";

        try {
            Mpa mpa = jdbcTemplate.queryForObject(sql, this::mapRowToMpa, id);
            return Optional.of(mpa);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Рейтинг MPA с ID {} не найден", id);
            return Optional.empty();
        }
    }

    private Mpa mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {
        return Mpa.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("mpa_name"))
                .build();
    }
}