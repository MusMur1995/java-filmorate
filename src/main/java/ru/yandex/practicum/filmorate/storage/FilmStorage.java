package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film create(Film film);
    Film update(Film film);
    Collection<Film> findAll();
    Optional<Film> findById(Integer id);
    // Дополнительные методы для лайков:
    void addLike(Integer filmId, Integer userId);
    void removeLike(Integer filmId, Integer userId);
    Collection<Film> getPopularFilms(int count);
}
