package raff.stein.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.document.model.entity.DocumentTypeEntity;

import java.util.Optional;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentTypeEntity, Integer> {

    Optional<DocumentTypeEntity> findByTypeName(String typeName);
}
