package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

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
        if (newFilm.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        if (!films.containsKey(newFilm.getId())) {
            throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
        }

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
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
        Set<Integer> filmLikes = likes.computeIfAbsent(filmId, k -> new HashSet<>());
        filmLikes.add(userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
        Set<Integer> filmLikes = likes.get(filmId);
        if (filmLikes == null) {
            throw new NotFoundException("У фильма с id = " + filmId + " нет лайков");
        }

        boolean removed = filmLikes.remove(userId);

        if (!removed) {
            throw new NotFoundException("Лайк от пользователя с id = " + userId + " не найден у фильма с id = " + filmId);
        }
    }

    @Override
    public Collection<Film> getPopularFilms(int count) {
        List<Film> allFilms = new ArrayList<>(films.values());

        if (allFilms.isEmpty()) {
            return Collections.emptyList();
        }

        allFilms.sort((film1, film2) -> {
            Set<Integer> likes1 = likes.get(film1.getId());
            int likesCount1 = (likes1 == null) ? 0 : likes1.size();

            Set<Integer> likes2 = likes.get(film2.getId());
            int likesCount2 = (likes2 == null) ? 0 : likes2.size();

            return Integer.compare(likesCount2, likesCount1);
        });

        if (count <= 0) {
            count = 10;
        }

        if (count > allFilms.size()) {
            count = allFilms.size();
        }

        return allFilms.subList(0, count);
    }
}