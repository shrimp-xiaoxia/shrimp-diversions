package com.shrimp.kv.biz.domain.repository;

import com.shrimp.kv.biz.domain.dataobject.NoteContentDO;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

/**
 * @author ccb
 */
public interface NoteContentRepository extends CassandraRepository<NoteContentDO, UUID> {
}
