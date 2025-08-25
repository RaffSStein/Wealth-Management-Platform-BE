package raff.stein.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.document.model.entity.FileValidationEntity;

@Repository
public interface FileValidationRepository extends JpaRepository<FileValidationEntity, Long> {
}
