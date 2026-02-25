package com.shrimp.kv.biz.domain.repository;

import com.shrimp.kv.biz.domain.dataobject.CommentContentDO;
import com.shrimp.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

/**
 * @author ccb
 */
public interface CommentContentRepository extends CassandraRepository<CommentContentDO, CommentContentPrimaryKey> {

    List<CommentContentDO> findByPrimaryKeyNoteIdAndPrimaryKeyYearMonthInAndPrimaryKeyContentIdIn(
            Long noteId,
            List<String> yearMoths,
            List<UUID> contentIds
    );
}
