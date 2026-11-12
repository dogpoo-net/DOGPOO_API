# 첨부파일 저장 경로

기본 루트: `/home/ocaptain/resource` (`WEAVER_RESOURCE_DIR`로 변경 가능).

- 메모 사진: `contents/note/image/<workspace UUID>/...`
- 메모 음성: `contents/note/audio/<workspace UUID>/...`
- 메모 영상용 예약 디렉터리: `contents/note/video`
- 문서 이미지용 예약 디렉터리: `contents/doc/image`
- 프로필: 기존 `profile/images` 유지

DB에는 루트 기준 상대 경로를 저장합니다. 업로드·조회·자료 삭제의 경로 검증도 새 구조를 사용합니다. 워크스페이스 삭제는 위 네 콘텐츠 디렉터리의 해당 UUID 하위만 삭제합니다.

2026-11-12 전환 전 기존 image/audio 파일 0개와 첨부 메타데이터 0건을 확인했습니다. 옮길 파일·DB 경로가 없어 데이터 마이그레이션은 수행하지 않았습니다. 영상 및 문서 이미지 업로드 API/UI 추가는 별도 기능 작업입니다.
