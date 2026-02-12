package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.film.Film;
import ru.yandex.practicum.filmorate.model.film.Mpa;
import ru.yandex.practicum.filmorate.model.user.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {
    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    private User getUser(Integer userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
    }

    private Film getFilm(Integer filmId) {
        return filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + filmId + " не найден"));
    }

    public Film create(Film film) {
        log.info("Создание фильма: {}", film.getName());
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }
        getFilm(film.getId());
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());
        return filmStorage.update(film);
    }

    public Film getFilmById(Integer id) {
        log.info("Получение фильма по ID: {}", id);
        return getFilm(id);
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.findAll();
    }

    public void addLike(Integer filmId, Integer userId) {
        getUser(userId);
        getFilm(filmId);
        filmStorage.addLike(filmId, userId);
    }

    public void deleteLike(Integer filmId, Integer userId) {
        getUser(userId);
        getFilm(filmId);
        filmStorage.removeLike(filmId, userId);
    }

    public Collection<Film> getPopularFilms(Integer count) {
        return filmStorage.getPopularFilms(count);
    }

    private void validateMpa(Mpa mpa) {
        if (mpa == null) {
            throw new ValidationException("Рейтинг MPA должен быть указан");
        }

        if (mpa.getId() == null) {
            throw new ValidationException("ID рейтинга MPA должен быть указан");
        }

        if (mpaStorage != null) {
            mpaStorage.findById(mpa.getId())
                    .orElseThrow(() -> new NotFoundException(
                            "Рейтинг MPA с id = " + mpa.getId() + " не найден"));
        }
    }

    private void validateGenres(Collection<ru.yandex.practicum.filmorate.model.film.Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        for (var genre : genres) {
            if (genre.getId() == null) {
                throw new ValidationException("ID жанра не может быть null");
            }

            genreStorage.findById(genre.getId())
                    .orElseThrow(() -> new NotFoundException(
                            "Жанр с id = " + genre.getId() + " не найден"));
        }
    }
}