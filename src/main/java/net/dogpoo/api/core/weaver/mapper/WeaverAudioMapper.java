package net.dogpoo.api.core.weaver.mapper;
import java.util.Map;import org.apache.ibatis.annotations.*;
@Mapper public interface WeaverAudioMapper {
 @Select("SELECT provider,model,enabled FROM WEAVER_AUDIO_SETTING WHERE id=1") Map<String,Object> setting();
 @Update("UPDATE WEAVER_AUDIO_SETTING SET provider=#{provider},model=#{model},enabled=#{enabled} WHERE id=1") void update(@Param("provider")String provider,@Param("model")String model,@Param("enabled")boolean enabled);
}
