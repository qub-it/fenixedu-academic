alter table CONTENT add column KEY_FUNCTIONALITY INTEGER(11);
alter table CONTENT add index (KEY_FUNCTIONALITY);

create temporary table MIGRATE_TO_FUNCTIONALITY_CALLS(
NAME VARCHAR(255),
CONTENT_ID VARCHAR(255),
KEY_NODE INTEGER(11),
KEY_FUNCTIONALITY INTEGER(11)
);

insert into MIGRATE_TO_FUNCTIONALITY_CALLS (NAME,CONTENT_ID,KEY_NODE,KEY_FUNCTIONALITY) 
 select C2.NAME, N.CONTENT_ID, N.ID_INTERNAL,  N.KEY_CHILD FROM NODE N, CONTENT C1, CONTENT C2 WHERE N.KEY_PARENT=C1.ID_INTERNAL AND N.KEY_CHILD=C2.ID_INTERNAL AND C1.OJB_CONCRETE_CLASS <> 'net.sourceforge.fenixedu.domain.functionalities.Module' AND C2.OJB_CONCRETE_CLASS='net.sourceforge.fenixedu.domain.functionalities.Functionality';


insert into CONTENT (NAME,CONTENT_ID,OJB_CONCRETE_CLASS,KEY_FUNCTIONALITY) 
select MFC.NAME, MFC.CONTENT_ID,'net.sourceforge.fenixedu.domain.contents.FunctionalityCall',MFC.KEY_FUNCTIONALITY
FROM MIGRATE_TO_FUNCTIONALITY_CALLS MFC;

update NODE N, CONTENT C set N.KEY_CHILD = C.ID_INTERNAL WHERE N.CONTENT_ID = C.CONTENT_ID;


drop temporary table MIGRATE_TO_FUNCTIONALITY_CALLS;