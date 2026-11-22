package ru.itmo.routes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.routes.model.ImportRecord;
import java.util.List;

public interface ImportRepository extends JpaRepository<ImportRecord, Long> {
    List<ImportRecord> findTop200ByIdLessThanOrderByIdDesc(long before);

    List<ImportRecord> findTop200ByUsernameAndIdLessThanOrderByIdDesc(String username, long before);
}
