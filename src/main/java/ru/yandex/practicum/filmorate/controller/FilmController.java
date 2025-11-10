package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final Map<Integer, Film> films = new HashMap<>();
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final String NAME_BLANK_MSG = "Название не может быть пустым";
    private static final String DESCRIPTION_TOO_LONG_MSG = "Максимальная длина описания — 200 символов";
    private static final String RELEASE_DATE_TOO_EARLY_MSG = "Дата релиза — не раньше 28 декабря 1895 года";
    private static final String DURATION_POSITIVE_MSG = "Продолжительность фильма должна быть положительным числом";

    //    добавление фильма;
    @PostMapping
    public Film createFilm(@RequestBody Film film) {

        log.info("Получен запрос на создание фильма: {}", film.getName());
        // проверяем выполнение необходимых условий
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Попытка создания фильма с пустым названием");
            throw new ValidationException(NAME_BLANK_MSG);
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Попытка создания фильма с описанием длиной {} символов", film.getDescription().length());
            throw new ValidationException(DESCRIPTION_TOO_LONG_MSG);
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Попытка создания фильма с некорректной датой релиза: {}", film.getReleaseDate());
            throw new ValidationException(RELEASE_DATE_TOO_EARLY_MSG);
        }

        if (film.getDuration() == null || film.getDuration() <= 0) {
            log.warn("Попытка создания фильма с некорректной продолжительностью: {}", film.getDuration());
            throw new ValidationException(DURATION_POSITIVE_MSG);
        }

        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Фильм создан успешно: {} (ID: {})", film.getName(), film.getId());
        return film;
    }

    // вспомогательный метод для генерации идентификатора нового user
    private int getNextId() {
        int currentMaxId = films.keySet()
                .stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    //    обновление фильма;
    @PutMapping
    public Film updateFilm(@RequestBody Film newFilm) {
        log.info("Получен запрос на обновление фильма с ID: {}", newFilm.getId());

        if (newFilm.getId() == null) {
            log.warn("Попытка обновления фильма без указания ID");
            throw new ValidationException("Id должен быть указан");
        }

        if (!films.containsKey(newFilm.getId())) {
            log.warn("Попытка обновления несуществующего фильма с ID: {}", newFilm.getId());
            throw new ValidationException("Фильм с id = " + newFilm.getId() + " не найден");
        }

        Film oldFilm = films.get(newFilm.getId());

        // Обновление полей с валидацией
        if (newFilm.getName() != null) {
            if (newFilm.getName().isBlank()) {
                log.warn("Попытка обновления фильма с пустым названием (ID: {})", newFilm.getId());
                throw new ValidationException(NAME_BLANK_MSG);
            }
            oldFilm.setName(newFilm.getName().trim());
            log.debug("Обновлено название фильма ID {}: {}", newFilm.getId(), newFilm.getName());
        }

        if (newFilm.getDescription() != null) {
            if (newFilm.getDescription().length() > 200) {
                log.warn("Попытка обновления фильма с описанием длиной {} символов (ID: {})",
                        newFilm.getDescription().length(), newFilm.getId());
                throw new ValidationException(DESCRIPTION_TOO_LONG_MSG);
            }
            oldFilm.setDescription(newFilm.getDescription().trim());
            log.debug("Обновлено описание фильма ID {}", newFilm.getId());
        }

        if (newFilm.getReleaseDate() != null) {
            if (newFilm.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
                log.warn("Попытка обновления фильма с некорректной датой релиза: {} (ID: {})",
                        newFilm.getReleaseDate(), newFilm.getId());
                throw new ValidationException(RELEASE_DATE_TOO_EARLY_MSG);
            }
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
            log.debug("Обновлена дата релиза фильма ID {}: {}", newFilm.getId(), newFilm.getReleaseDate());
        }

        if (newFilm.getDuration() != null) {
            if (newFilm.getDuration() <= 0) {
                log.warn("Попытка обновления фильма с некорректной продолжительностью: {} (ID: {})",
                        newFilm.getDuration(), newFilm.getId());
                throw new ValidationException(DURATION_POSITIVE_MSG);
            }
            oldFilm.setDuration(newFilm.getDuration());
            log.debug("Обновлена дата релиза фильма ID {}: {}", newFilm.getId(), newFilm.getReleaseDate());
        }
        log.info("Фильм с ID {} успешно обновлен", newFilm.getId());
        return oldFilm;
    }

    //    получение всех фильмов.
    @GetMapping
    public Collection<Film> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов. Количество фильмов: {}", films.size());
        return films.values();
    }
}