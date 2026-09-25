package com.simplechat.common;

import com.simplechat.exception.ApiException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;

public interface EntityService<TEntity, TIdType> {

    JpaRepository<TEntity, TIdType> getRepository();

    default Optional<TEntity> findById(TIdType id) {
        return Optional.ofNullable(id).map(value -> getRepository().findById(value))
                       .orElseThrow(this::getNullIdException);
    }

    default RuntimeException getNullIdException() {
        return new RuntimeException(String.format("%s. NULL id provided", getClass().getSimpleName()));
    }

    default TEntity getById(TIdType id) {
        return findById(id).orElseThrow(() -> getNotFoundByIdException(id));
    }

    default ApiException getNotFoundByIdException(TIdType id) {
        return new ApiException(
            HttpStatus.NOT_FOUND.value(),
            String.format("%s. Object with id '%s' not found", getClass().getSimpleName(), id)
        );
    }

    default TEntity save(TEntity entity) {
        return getRepository().save(entity);
    }

    default List<TEntity> saveAll(List<TEntity> entities) {
        return getRepository().saveAll(entities);
    }

}
