package net.dogpoo.api.core.convatar.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Select;

import net.dogpoo.api.core.convatar.model.ConvatarNoticeModel;

public interface ConvatarHomeMapper {

	@Select("SELECT idx, ver, contents, DATE_FORMAT(regDate, '%Y-%m-%d') AS regDate FROM CONVATAR_RELEASE_NOTES WHERE isShow ORDER BY idx DESC")
	public List<HashMap<String, Object>> getConvatarReleaseNotesList();
	
	@Select("SELECT idx, title, contents, writer, DATE_FORMAT(regDate, '%Y-%m-%d') AS regDate, DATE_FORMAT(regDate, '%T') AS regTimestamp FROM CONVATAR_MAIN_NOTICE WHERE isShow ORDER BY idx DESC")
	public List<ConvatarNoticeModel> getNoticeModelList();
	
}
