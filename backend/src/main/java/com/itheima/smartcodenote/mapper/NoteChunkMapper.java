package com.itheima.smartcodenote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.smartcodenote.entity.NoteChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoteChunkMapper extends BaseMapper<NoteChunk> {

    /**
     * Delete all chunks for a given note.
     */
    @Delete("DELETE FROM note_chunk WHERE note_id = #{noteId}")
    int deleteByNoteId(@Param("noteId") Long noteId);
}
