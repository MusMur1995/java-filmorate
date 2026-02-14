package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.film.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Integer, Film> films = new HashMap<>();
    private final Map<Integer, Set<Integer>> likes = new HashMap<>();

    @Override
    public Film create(Film film) {
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    private int getNextId() {
        int currentMaxId = films.keySet()
                .stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @Override
    public Film update(Film newFilm) {

        films.put(newFilm.getId(), newFilm);
        return newFilm;
    }

    @Override
    public Optional<Film> findById(Integer id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        Set<Integer> filmLikes = likes.computeIfAbsent(filmId, k -> new HashSet<>());
        filmLikes.add(userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        Set<Integer> filmLikes = likes.get(filmId);
        if (filmLikes != null) {
            filmLikes.remove(userId);
        }
    }

    @Override
    public Collection<Film> getPopularFilms(int count) {
        return films.values().stream()
                .sorted(this::compareFilmsByLikes)
                .limit(count <= 0 ? 10 : count)
                .collect(Collectors.toList());
    }

    private int compareFilmsByLikes(Film film1, Film film2) {
        int likesCount1 = getLikesCount(film1.getId());
        int likesCount2 = getLikesCount(film2.getId());
        return Integer.compare(likesCount2, likesCount1);
    }

    private int getLikesCount(Integer filmId) {
        Set<Integer> filmLikes = likes.get(filmId);
        return filmLikes == null ? 0 : filmLikes.size();
    }
}