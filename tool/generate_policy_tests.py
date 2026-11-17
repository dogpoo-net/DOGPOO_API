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
('소유 수','초대받은 공간 제외, 활성 소유 공간만 계산','수치 미정: NULL은 추가 제한 없음'),
('0개 상태','마지막 삭제·이전 허용, 자동 재생성 금지','생성·복원·초대 수락 화면 제공'),
('무료 삭제','사용한 공간은 이메일 인증 후 즉시 영구 삭제','파일은 기존 삭제 큐로 처리; API 접근 즉시 차단'),
('유료 삭제','사용한 공간은 30일 보관','미사용 공간은 기존 정책대로 바로 삭제'),
('복원','보관 기한 내 유료 권한 및 소유·자료 한도 검사','다운그레이드 전 삭제한 자료는 기존 기한 유지'),
('구독 귀속','계정이 아닌 워크스페이스별','계정 상세에서 소유 공간 선택 후 조정'),
('취소','종료일 유지, 종료 시 Free','취소 예약 해제는 종료일을 연장하지 않음'),
('만료','DB UTC 기준 요청마다 판정','스케줄러 지연과 무관; 자동 결제 연장 없음'),
('다운그레이드','기존 자료 보존, 초과하는 추가 사용 제한','소유 한도·편집자 초과 시 우선 대상 선택 가능'),
('기존 자료 수정','문서 수 같으면 수정 허용; 용량 증가 제한','현재 자료+첨부 기준; 이력은 기존 운영 한도'),
('기본 한도','Free 편집자 소유자 포함 2명, 조회자 무제한','나머지 수치는 미정; 운영 저장 상한 64MiB 유지'),
('관리자','설정에서 관리자 백오피스 진입','일반 사용자에게 링크 숨김 + 서버 권한 검사'),
('나가기','이전에 있던 워크스페이스로 복귀','설정 재진입·로그아웃·공간 전환 없음'),
('초기 관리자','dogpoo.net@gmail.com','검증된 기존 계정에 서버에서 지정'),
('관리 변경','계정 역할·공간별 구독·멤버 역할','마지막 관리자 해제 방지, 변경 이력 기록'),
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
case('생성','한도 미정 상태에서 생성','임의의 새 한도 없음',u+'.unspecifiedLimitsRemainUnlimited')
case('소유 한도','한도 1일 때 1개 소유 후 생성','서버가 거부',u+'.ownedLimitBlocksAdmissionAtBoundary')
case('소유 한도','두 기기에서 동시에 마지막 생성 자리 사용','한 요청만 성공, 소유 한도 유지',None,priority='P0')
case('소유 한도','미리보기 후 다른 공간 생성하고 복원','복원 시 한도 재검사')
case('소유 한도','수신자가 한도 초과인 상태에서 소유권 이전','이전 거부, 기존 소유권 보존')
case('소유 한도','마지막 소유 공간 이전','이전 허용, 자동 재생성 없음')
case('소유 한도','구독 만료 후 소유 한도 초과','자료 보존; 우선 공간 외 조회 전용')
case('소유 한도','사용할 공간 선택 변경','선택 우선순위가 서버에 저장되고 제한 적용')
case('소유 한도','타인 소유/중복 ID를 선택 API에 전송','서버 거부, 우선순위 원복')
case('삭제','사용한 Free 공간을 이메일 인증 후 삭제','보관 없이 DB 삭제·파일 삭제 큐',u+'.usedFreeSpaceDeletesPermanentlyAfterVerification',priority='P0')
case('삭제','사용한 유료 공간을 이메일 인증 후 삭제','30일 보관, 즉시 파일 삭제 안 함',u+'.paidDeletionRetainsThirtyDays',priority='P0')
case('삭제','생성 직후 미사용 공간 삭제','인증 없이 영구 삭제','WeaverWorkspaceLifecycleTest.unusedSpaceDeletesImmediatelyWithoutMail')
case('삭제','메일 인증 없이 사용한 공간 삭제','서버 거부','WeaverWorkspaceLifecycleTest.usedSpaceCannotBypassVerificationEvenAfterEmptyPreview',priority='P0')
case('삭제','다른 사용자·공간의 인증코드 사용','서버 거부','WeaverWorkspaceLifecycleTest.codeIsBoundToOwnerWorkspaceAndCurrentEmail',priority='P0')
case('삭제','인증코드 시도 횟수 초과','더 이상 인증 불가','WeaverWorkspaceLifecycleTest.exhaustedAttemptsCannotVerify')
case('삭제','Free 삭제 모달 표시','복원 불가·즉시 영구 삭제 문구',f+'.free used workspace clearly warns irreversible deletion')
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
case('멤버','Free 공간에서 세 번째 편집자 추가','소유자 포함 2명 제한',u+'.freeHasOwnerAndOneEditor')
case('멤버','읽기 전용 사용자 여러 명 추가','편집자 자리 소모 없음')
case('멤버','구독 만료로 편집자 초과','안정된 순서로 허용 인원만 편집, 나머지 조회')
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
case('구독','동일 계정의 두 공간 중 하나만 플랜 변경','선택 공간만 변경, 다른 공간 불변')
case('관리자','일반 계정으로 관리자 API 직접 호출','조회·권한 변경·구독 변경 거부',u+'.ordinaryUserCannotUseAdminQueriesOrMutations',priority='P0')
case('관리자','마지막 관리자 해제','서버 거부',u+'.lastAdministratorCannotBeDemoted',priority='P0')
case('관리자','일반 사용자를 관리자로 지정','지정 및 변경 이력 기록',u+'.administratorRoleChangesAreAudited')
case('관리자','권한 해제 후 열려 있던 백오피스에서 요청','다음 요청부터 서버 거부')
case('관리자','계정 검색 후 다른 계정 선택','이메일·소유 공간·구독이 선택 계정 기준')
case('관리자','구독 종료일을 과거/잘못된 문자열로 지정','서버 거부',u+'.invalidAdminSubscriptionCannotGrantAccess')
case('관리자','공간 플랜을 Free로 변경','즉시 Free 적용, 본문 보존')
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
for row in rows:ws.append(row)
settings=wb.create_sheet('플랜 설정');settings.append(['플랜','소유 활성 공간','공간별 bytes','문서 수','편집자 (소유자 포함)','읽기 전용','삭제 보관'])
settings.append(['Free','미정 / NULL','미정 / NULL','미정 / NULL',2,'제한 없음','없음 / 즉시 영구 삭제'])
for plan in ['Standard','Pro']:settings.append([plan,'미정 / NULL','미정 / NULL','미정 / NULL','미정 / NULL','제한 없음','30일 (사용한 공간)'])
settings.append(['운영 상한','별도','기존 64MiB 유지','별도 없음','별도 없음','',''])
results=wb.create_sheet('검증 결과');results.append(['검증','상태','범위·한계'])
for row in [('정책 단위 테스트 19개','통과','인증·무료/유료 삭제·한도·취소·관리자 보호'),('MariaDB 정책 통합 테스트 3개','통과','실제 SQL·UTC 만료·취소일 유지·최초 생성·기본 설정'),('관련 Flutter 테스트 28개','통과','백오피스 나가기·권한 오류·삭제 문구·기존 워크스페이스 UI'),('브라우저/실제 기기 수동 검증','미실행','테스트 케이스의 미실행 항목을 별도로 수행'),('실제 결제·환불·웹훅','미구현','사용자 요청 범위는 서버 구독 정책까지'),('운영 플랜 숫자/가격','미정','임의로 한도를 정하지 않음')]:results.append(row)
ops=wb.create_sheet('운영 가이드');ops.append(['항목','내용'])
for row in [('관리 화면','관리자 계정 → 설정 → 관리자 백오피스'),('나가기','이전에 사용하던 워크스페이스로 복귀'),('구독 변경','계정 검색 → 소유 공간 선택 → 플랜·구독 상태 변경'),('유료 종료일','시간대 포함 ISO 형식 (예: 2027-01-01T00:00:00+09:00)'),('초기 관리자','dogpoo.net@gmail.com'),('한도 설정','WEAVER_PLAN_POLICY의 nullable limit 컬럼. 숫자는 확정 후 변경'),('보관 정리','매일 00:00 Asia/Seoul; 복원 유효기간 검사는 요청 시 수행'),('비밀정보','엑셀에 비밀번호·API키·실제 사용자 본문을 기록하지 말 것'),('수동 검증','테스트용 계정/공간을 이용하고 운영 자료 삭제 금지'),('반복 생성','tool/generate_policy_tests.py'),('명세','docs/workspace-subscription-policy.md')]:ops.append(row)
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
