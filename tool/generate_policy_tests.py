"""Generate the QA workbook without production data or credentials."""
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo
from openpyxl import Workbook, load_workbook
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.utils import get_column_letter

out=Path('/tmp/WEAVER_workspace_policy_tests.xlsx')
wb=Workbook();summary=wb.active;summary.title='정책 요약'
summary.append(['영역','정책','비고'])
policies=[
('최초 생성','가입 시 기본 워크스페이스 1회 생성','기존 계정은 마이그레이션 시 최초 생성 완료로 표시'),
('소유 수','초대받은 공간 제외, 활성 소유 공간만 계산','Free 2 / Standard 10 / Pro 무제한'),
('0개 상태','마지막 삭제·이전 허용, 자동 재생성 금지','생성·복원·초대 수락 화면 제공'),
('무료 삭제','30일 보관, 유료 전환 후 복원 가능','만료 후 삭제 큐 처리; 보관 중 접근 차단'),
('유료 삭제','사용한 공간은 30일 보관','미사용 공간도 인증 없이 30일 보관'),
('복원','보관 기한 내 유료 권한 및 소유·자료 한도 검사','다운그레이드 전 삭제한 자료는 기존 기한 유지'),
('구독 귀속','계정 단위, 소유 공간 전체에 적용','계정 상세에서 구독 조정'),
('취소','종료일 유지, 종료 시 Free','취소 예약 해제는 종료일을 연장하지 않음'),
('만료','DB UTC 기준 요청마다 판정','스케줄러 지연과 무관; 자동 결제 연장 없음'),
('다운그레이드','기존 자료 보존, 초과하는 추가 사용 제한','소유 한도·편집자 초과 시 우선 대상 선택 가능'),
('기존 자료 수정','문서 수 같으면 수정 허용; 용량 증가 제한','현재 자료+첨부 기준; 이력은 기존 운영 한도'),
('기본 한도','Free 편집자 소유자 포함 2명, 조회자 무제한','나머지 수치는 미정; 운영 저장 상한 64MiB 유지'),
('관리자','설정에서 관리자 백오피스 진입','일반 사용자에게 링크 숨김 + 서버 권한 검사'),
('나가기','이전에 있던 워크스페이스로 복귀','설정 재진입·로그아웃·공간 전환 없음'),
('초기 관리자','dogpoo.net@gmail.com','검증된 기존 계정에 서버에서 지정'),
('관리 변경','계정 역할·계정 구독·멤버 역할','마지막 관리자 해제 방지, 변경 이력 기록'),
('결제','서버 정책만 구현','결제·환불·실제 갱신·웹훅은 미연동'),
]
for row in policies:summary.append(row)
ws=wb.create_sheet('테스트 케이스');ws.append(['ID','영역','우선순위','사전 조건','절차','기대 결과','검증 방법','결과','자동검증 근거','담당자','실행일','실제 결과·이슈'])
rows=[]
def case(area,title,expected,auto=None,pre='테스트 계정·워크스페이스 사용',priority='P1'):
 rows.append([f'WP-{len(rows)+1:03}',area,priority,pre,title,expected,'자동' if auto else '수동','자동검증 통과' if auto else '미실행',auto or '', '',datetime.now(ZoneInfo('Asia/Seoul')).strftime('%Y-%m-%d') if auto else '',''])
u='WeaverWorkspacePolicyTest';i='WeaverPolicyIntegrationTest';f='admin_backoffice_test.dart'
case('생성','이메일 신규 가입','기본 워크스페이스가 1회 생성됨',None,priority='P0')
case('생성','소셜 신규 가입','기본 워크스페이스가 1회 생성됨',None,priority='P0')
case('생성','동일 계정 초기화 두 번 호출','기본 공간 생성은 한 번',u+'.newAccountsAreProvisionedExactlyOnce',priority='P0')
case('생성','기존 계정/삭제 후 ensure 재호출','자동 재생성 없음',i+'.existingAccountsNeverAutoProvisionAndNewAccountsProvisionOnce',priority='P0')
case('생성','빈 catalog 요청','defaultWorkspaceId=null, 생성 쓰기 없음',u+'.emptyCatalogDoesNotCreateAWorkspace',priority='P0')
case('생성','마지막 공간 삭제 후 재로그인·새로고침','빈 공간 안내; 과거 자료 노출 없음',None,priority='P0')
case('생성','초대받은 공간만 남긴 뒤 접속','초대 공간 사용; 소유 수 0',u+'.joinedMembershipsNeverConsumeOwnedQuota')
case('생성','빈 화면에서 초대 수락','새 기본 공간 없이 초대 공간 진입')
case('생성','빈 화면에서 새 공간 생성/복원','해당 공간 진입; 로딩 무한 반복 없음')
case('생성','플랜별 활성 소유 공간 제한','Free 2 / Standard 10 / Pro 무제한')
case('소유 한도','한도 1일 때 1개 소유 후 생성','서버가 거부',u+'.ownedLimitBlocksAdmissionAtBoundary')
case('소유 한도','두 기기에서 동시에 마지막 생성 자리 사용','한 요청만 성공, 소유 한도 유지',None,priority='P0')
case('소유 한도','미리보기 후 다른 공간 생성하고 복원','복원 시 한도 재검사')
case('소유 한도','수신자가 한도 초과인 상태에서 소유권 이전','이전 거부, 기존 소유권 보존')
case('소유 한도','마지막 소유 공간 이전','이전 허용, 자동 재생성 없음')
case('소유 한도','구독 만료 후 소유 한도 초과','자료 보존; 우선 공간 외 조회 전용')
case('소유 한도','사용할 공간 선택 변경','선택 우선순위가 서버에 저장되고 제한 적용')
case('소유 한도','타인 소유/중복 ID를 선택 API에 전송','서버 거부, 우선순위 원복')
case('삭제','사용한 Free 공간을 이메일 인증 후 삭제','30일 보관; 유료 전환 후 복원',u+'.usedFreeSpaceRetainsThirtyDaysAfterVerification',priority='P0')
case('삭제','사용한 유료 공간을 이메일 인증 후 삭제','30일 보관, 즉시 파일 삭제 안 함',u+'.paidDeletionRetainsThirtyDays',priority='P0')
case('삭제','생성 직후 미사용 공간 삭제','인증 없이 30일 보관')
case('삭제','메일 인증 없이 사용한 공간 삭제','서버 거부','WeaverWorkspaceLifecycleTest.usedSpaceCannotBypassVerificationEvenAfterEmptyPreview',priority='P0')
case('삭제','다른 사용자·공간의 인증코드 사용','서버 거부','WeaverWorkspaceLifecycleTest.codeIsBoundToOwnerWorkspaceAndCurrentEmail',priority='P0')
case('삭제','인증코드 시도 횟수 초과','더 이상 인증 불가','WeaverWorkspaceLifecycleTest.exhaustedAttemptsCannotVerify')
case('삭제','Free 삭제 모달 표시','30일 보관·유료 복원 문구')
case('삭제','공유 공간 삭제 후 참여자가 읽기/쓰기','권한 거부, 자료 재생성 없음',None,priority='P0')
case('삭제','파일 삭제 큐 실패 후 재시도','자료 노출 없이 재시도 처리')
case('복원','Free 상태의 보관 공간 복원','유료 권한 필요',u+'.restorationRequiresPaidEntitlement')
case('복원','유료 공간의 용량/문서 수 한도 초과','복원 거부',u+'.restoreRejectsStorageAndDocumentOverflow')
case('복원','30일 종료 후 복원','복원 거부','WeaverWorkspaceLifecycleTest.restoreRequiresRemainingRetentionPeriod')
case('복원','보관 공간에서 구독 만료','삭제 예정일 유지; 유료 재활성화 필요')
case('복원','삭제 보관 중 재구독하고 복원','기존 내용·멤버·이력 유지')
case('복원','매일 한국 자정 만료 정리','만료 자료만 삭제; 복원과 경합 시 재검사','WeaverWorkspaceLifecycleTest.purgeRechecksDeadlineWhileLocked')
case('복원','만료된 구독 보관 공간 UI','복원 버튼 비활성화·유료 구독 필요 표시',f+'.expired archived space exposes no enabled restore action')
case('멤버','무료 계정이 유료 공간 편집자로 참여','공간 플랜의 편집 권한 사용',u+'.paidSpaceAllowsFreeAccountEditorWithUnlimitedPaidSeats')
case('멤버','Free 공간에서 초대 요청','오프라인 공간 공유 거부')
case('멤버','읽기 전용 사용자 여러 명 추가','편집자 자리 소모 없음')
case('멤버','구독 만료로 편집자 초과','Free 소유 공간은 오프라인 전환, 참여자 접근 중단')
case('멤버','소유자가 편집자 유지 대상을 선택','초과·외부 계정 거부, 선택 역할 적용',u+'.editorSelectionRejectsOutsidersAndKeepsOwnerSeat')
case('멤버','만료 전에 발급한 초대 수락','현재 한도 기준 재검사')
case('멤버','구독 재활성화','기존 설정 역할의 편집 권한 회복')
case('멤버','조회자/과거 편집자가 직접 save 호출','서버에서 거부',None,priority='P0')
case('용량','문서 수 초과 상태에서 기존 문서 수정','개수 증가 없으면 허용',u+'.documentOverflowAllowsSameCountEditsButNotNewDocuments')
case('용량','문서 수 초과 상태에서 새 문서 추가','트랜잭션 전체 원복',u+'.documentOverflowAllowsSameCountEditsButNotNewDocuments')
case('용량','용량 초과 상태에서 내용을 줄임','저장 허용',u+'.storageOverflowAllowsShrinkAndRejectsGrowth')
case('용량','용량 초과 상태에서 내용/첨부 추가','증가 거부',u+'.storageOverflowAllowsShrinkAndRejectsGrowth')
case('용량','여러 변경을 묶어 저장하며 한도 초과','일부 저장 없이 전체 원복')
case('용량','휴지통 복원으로 문서/용량 증가','동일한 한도 적용')
case('구독','취소 예약 설정','종료일 변경 없이 유료 유지',u+'.cancellationIsOwnerOnlyAndDoesNotChangeEndDate',priority='P0')
case('구독','종료 시각을 경과시킴','배치 없이 effective plan Free',i+'.expiryImmediatelyChangesEffectivePlanAndCancellationKeepsPeriod',priority='P0')
case('구독','취소 예약 해제','기간 연장이나 결제 없이 예약만 해제')
case('구독','소유자 아닌 참여자가 취소 요청','권한 거부',u+'.cancellationIsOwnerOnlyAndDoesNotChangeEndDate')
case('구독','서버 재기동 후 구독 조회','종료일·취소 상태 유지')
case('구독','유료 기간이 끝난 공간 조회','기존 데이터 접근 유지, 자동 삭제 없음')
case('구독','동일 계정의 구독 플랜 변경','소유 공간 전체에 동일 플랜 적용')
case('관리자','일반 계정으로 관리자 API 직접 호출','조회·권한 변경·구독 변경 거부',u+'.ordinaryUserCannotUseAdminQueriesOrMutations',priority='P0')
case('관리자','마지막 관리자 해제','서버 거부',u+'.lastAdministratorCannotBeDemoted',priority='P0')
case('관리자','일반 사용자를 관리자로 지정','지정 및 변경 이력 기록',u+'.administratorRoleChangesAreAudited')
case('관리자','권한 해제 후 열려 있던 백오피스에서 요청','다음 요청부터 서버 거부')
case('관리자','계정 검색 후 다른 계정 선택','이메일·소유 공간·구독이 선택 계정 기준')
case('관리자','구독 종료일을 과거/잘못된 문자열로 지정','서버 거부',u+'.invalidAdminSubscriptionCannotGrantAccess')
case('관리자','계정 플랜을 Free로 변경','즉시 Free 적용, 본문 보존')
case('관리자','관리자가 멤버 역할을 변경','한도 검사·역할 반영·감사 이력')
case('관리자','멤버 역할 API로 소유자 변경 시도','거부; 소유권 이전 경로 필요')
case('관리자','감사 이력 확인','행위자·대상·시각·이전/이후 상태 확인',i+'.defaultLimitsRemainUnsetExceptFreeEditorCountAndAdminAuditPersists')
case('UI','일반 계정 설정 열기','백오피스 링크 없음')
case('UI','관리자 계정 설정 열기','백오피스 링크 표시')
case('UI','백오피스 나가기','기존 워크스페이스로 복귀, 로그아웃 없음',f+'.admin exit returns to prior workspace without logging out')
case('UI','일반 계정으로 관리자 화면 진입','오류 표시, 관리 데이터 없음',f+'.ordinary account admin denial shows error instead of data')
case('UI','다크/라이트 및 모바일/데스크탑 표시','위버 기존 테마, 스크롤·버튼 넘침 없음')
case('UI','백오피스 진입 전 작성 중인 내용 존재','나가기 후 내용·선택 공간 유지')
case('회귀','메모/문서/캔버스 CRUD·다기기 동기화','기존 동작 유지')
case('회귀','게스트 자료 가져오기 후 소유 공간 0개','초대 공간으로 자동 업로드하지 않음')
case('회귀','문서 충돌 자동/수동 병합','정책 검사와 기존 병합 모두 동작')
case('회귀','로그아웃·계정 교체 중 관리자 요청 완료','다른 계정으로 관리 데이터 노출 없음')
case('생성','Free 생성 후 24시간 이내 삭제·재생성','거부; 삭제로 시간 초기화되지 않음')
case('생성','Free 생성 후 정확히 24시간 경과','공간 한도 내에서 생성 허용')
case('오프라인','Free 소유 공간 내용 변경','기기에만 저장, 서버 쓰기 없음')
case('오프라인','로그아웃 후 다른 계정 로그인','이전 계정의 오프라인 자료 노출 없음')
case('오프라인','Free 사용자가 초대받은 유료 공간 접속','소유자의 유료 온라인 권한 사용')
case('오프라인','유료 전환 후 오프라인 자료 존재','확인 후 변경 업로드; 거절/실패 시 로컬 유지')
case('파일','Free 파일 가져오기','현재 공간만 교체, 다른 공간 및 계정 보존','offline_workspace_test.dart')
case('파일','손상된 파일 가져오기','기존 자료 유지','offline_workspace_test.dart')
case('파일','유료 가져오기 직전 다른 기기가 저장','버전 충돌로 거부, 기존 내용 유지')
case('파일','유료 파일 가져오기','현재 공간 서버·로컬 교체, 다른 공간 불변')
case('파일','내보낸 첨부 포함 파일 다시 가져오기','메모·문서·배치·음성·사진 복원')
case('음성','키/ffmpeg 미설정 상태에서 활성화','거부, 비밀 값 노출 없음','WeaverAudioTranscriptionTest')
case('음성','Free 12초 샘플 변환','10초만 변환, 공급자 모의 호출','WeaverAudioTranscriptionTest')
case('음성','61초 무료 / 3601초 유료 녹음','기간 초과 거부','WeaverAudioTranscriptionTest')
case('음성','백오피스 모델 전환','Groq turbo/large 또는 OpenAI mini 저장')
case('음성','공급자 실제 변환','API키와 변환도구 설정 후 정확도/비용 검증')
case('메뉴','기록함과 상단 아이콘 위치','핀·모두접기·닫기가 같은 첫 행 오른쪽')
case('동기화','오프라인 후 온라인으로 전환','생각과 메모상자 배치를 서버 기준으로 갱신, 이전 사본 보존')
case('동기화','같은 항목의 서버/오프라인 동시 변경','자동 덮어쓰기 금지, 수동 비교로 해결')
case('동기화','휴지통 위 동기화 메뉴 열기','유료 소유 공간에서 비교 화면 표시')
case('동기화','로컬 변경 버리고 서버 사용 선택','서버 쓰기 없이 로컬만 갱신')
case('동기화','선택 후 다른 기기에서 서버 변경','버전 충돌로 중단, 다시 비교')
case('캔버스','생각·상자·폴더 이동 후 이전 서버 응답 도착','위치 유지, 확인 이후 다른 참여자 이동은 정상 수신')
case('관리자','이메일 일부 또는 이름 입력','인풋 아래 어시스트 5개, 방향키·엔터 선택')
case('구독','9999-12-31 종료일 저장·조회','9999-12-31 23:59:59 한국 시간, 다음 연도 넘침 없음')
# Manual cases are never reported as executed. Existing automatic references are
# evidence pointers, not a substitute for the current run logs.
for row in rows:
 if row[6]=='자동': row[7]='미실행';row[10]=''
 ws.append(row)
settings=wb.create_sheet('플랜 설정');settings.append(['플랜','소유 활성 공간','공간별 bytes','문서 수','편집자','모드','삭제 보관'])
for plan,count,mode in [('Free',2,'오프라인'),('Standard',10,'온라인'),('Pro','무제한','온라인')]:settings.append([plan,count,'미정 / NULL','미정 / NULL','Free 공유 중단 / 유료 미정',mode,'30일, 유료만 복원'])
results=wb.create_sheet('검증 결과');results.append(['검증','상태','범위·한계'])
for row in [('백엔드 자동 테스트','125 통과 / 31 조건부 제외','총 156; MariaDB 정책·검색 5개, 오프라인 반영 충돌 방어 포함'),('Flutter 관련 테스트','61 통과','정적 분석도 통과; 실제 기기 수동 검증은 별도'),('브라우저/실제 기기 수동 검증','미실행','실제 기기 및 UI 수동 검증 필요'),('실제 결제·환불·웹훅','미구현','서버 구독 정책만 구현'),('실제 음성 공급자 호출','미실행','키·ffmpeg 설정 필요; 합성 음원과 모의 응답만 테스트'),('운영 가격·저장/문서 한도','미정','소유 공간 한도 2/10/무제한은 확정')]:results.append(row)
ops=wb.create_sheet('운영 가이드');ops.append(['항목','내용'])
for row in [('관리 화면','관리자 → 설정 → 백오피스'),('나가기','이전에 사용하던 워크스페이스로 복귀'),('구독 변경','계정 검색 → 계정 구독 변경 → 소유 공간 전체 적용'),('초기 관리자','dogpoo.net@gmail.com'),('보관 정리','매일 00:00 Asia/Seoul; 30일 만료 삭제'),('로컬 백업','Free 기기 자료는 서버 보관으로 복구되지 않음. 파일 백업 필요'),('음성','GROQ_API_KEY / OPENAI_API_KEY 및 ffmpeg·ffprobe 필요'),('비밀정보','키·비밀번호·사용자 본문을 엑셀에 기록하지 말 것'),('수동 검증','테스트용 계정·공간만 사용'),('명세','docs/workspace-subscription-policy.md, docs/audio-transcription.md')]:ops.append(row)
for sheet in wb:
 sheet.freeze_panes='A2';sheet.auto_filter.ref=sheet.dimensions
 sheet.sheet_view.showGridLines=False
 for cell in sheet[1]:cell.fill=PatternFill('solid',fgColor='263D52');cell.font=Font(name='맑은 고딕',bold=True,color='FFFFFF');cell.alignment=Alignment(wrap_text=True,vertical='center')
 for row in sheet.iter_rows(min_row=2):
  for cell in row:cell.font=Font(name='맑은 고딕',size=10);cell.alignment=Alignment(wrap_text=True,vertical='top')
  if row[0].row%2==0:
   for cell in row:cell.fill=PatternFill('solid',fgColor='F0F5FA')
 sheet.print_title_rows='1:1';sheet.sheet_properties.pageSetUpPr.fitToPage=True;sheet.page_setup.orientation='landscape';sheet.page_setup.paperSize=sheet.PAPERSIZE_A3;sheet.page_setup.fitToWidth=1;sheet.page_setup.fitToHeight=0
 widths=[12,16,10,32,42,48,12,20,54,16,16,42] if sheet==ws else [24]+[48]*(sheet.max_column-1)
 for index,width in enumerate(widths,1):sheet.column_dimensions[get_column_letter(index)].width=width
 for index in range(2,sheet.max_row+1):sheet.row_dimensions[index].height=46 if sheet==ws else 42
 sheet.row_dimensions[1].height=30
validation=DataValidation(type='list',formula1='"미실행,자동검증 통과,수동검증 통과,실패,보류"');ws.add_data_validation(validation);validation.add(f'H2:H{ws.max_row}')
wb.save(out)
check=load_workbook(out);assert len(check.worksheets)==5 and check['테스트 케이스'].max_row==len(rows)+1
print(f'{out}: {len(rows)} cases, {len(check.worksheets)} sheets')
