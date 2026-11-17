-- Stop the API first; hydrate legacy payloads before running the previous jar.
ALTER TABLE WEAVER_WORKSPACE_ITEM ADD COLUMN IF NOT EXISTS payload LONGTEXT NULL;
START TRANSACTION;
UPDATE WEAVER_WORKSPACE_ITEM w LEFT JOIN WEAVER_MEMO_CONTENT c0 ON c0.workspace_id=w.user_id AND c0.item_key=w.item_key LEFT JOIN WEAVER_DOCUMENT_CONTENT c1 ON c1.workspace_id=w.user_id AND c1.item_key=w.item_key LEFT JOIN WEAVER_CANVAS_LAYOUT c2 ON c2.workspace_id=w.user_id AND c2.item_key=w.item_key LEFT JOIN WEAVER_WORKSPACE_STRUCTURE c3 ON c3.workspace_id=w.user_id AND c3.item_key=w.item_key LEFT JOIN WEAVER_WORKSPACE_SETTING c4 ON c4.workspace_id=w.user_id AND c4.item_key=w.item_key
SET w.payload=IF(w.deleted,NULL,COALESCE(c0.payload,c1.payload,c2.payload,c3.payload,c4.payload));
COMMIT;
-- Keep child tables for reapplying the forward migration. No tables are dropped.
