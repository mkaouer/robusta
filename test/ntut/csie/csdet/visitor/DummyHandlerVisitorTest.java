package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.filemaker.exceptionBadSmells.DummyHandlerExampleWithTryStatementInNonTryStatement;
import ntut.csie.filemaker.exceptionBadSmells.DummyHandlerWithNestedTryStatement;
import ntut.csie.filemaker.exceptionBadSmells.UserDefineDummyHandlerFish;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DummyHandlerVisitorTest {
	String testProjectName;
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	DummyHandlerVisitor dummyHandlerVisitor;
	SmellSettings smellSettings;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	String[] dummyHandlerPatternsInXML;

	public DummyHandlerVisitorTest() {
		testProjectName = "DummyHandlerTest";
	}
	
	@Before
	public void setUp() throws Exception {
		// �ǳƴ����ɮ׼˥����e
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// �s�W�����J��library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");

		// �Yexample code����robustness notation�h������i�H���sĶ�q�L
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);

		// �إ߷s���ɮ�DummyAndIgnoreExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyAndIgnoreExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		// �~��إߴ��եΪ�UserDefineDummyHandlerFish
		javaFile2String.clear();
		javaFile2String.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefineDummyHandlerFish.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		// �إ�XML
		dummyHandlerPatternsInXML = new String[] {
				SmellSettings.EXTRARULE_ePrintStackTrace, SmellSettings.EXTRARULE_SystemErrPrint, 
				SmellSettings.EXTRARULE_SystemErrPrintln, SmellSettings.EXTRARULE_SystemOutPrint, 
				SmellSettings.EXTRARULE_SystemOutPrintln, SmellSettings.EXTRARULE_JavaUtilLoggingLogger, 
				SmellSettings.EXTRARULE_OrgApacheLog4j};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);

		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
	}

	@After
	public void tearDown() throws Exception {
		deleteOldSettings();
		// �R���M��
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testVisitMethodInvocation() {
		MethodInvocation methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"true_printStackTrace_public", "e.printStackTrace()").get(0);
		assertFalse(dummyHandlerVisitor.visit(methodInvocation));
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testDetectDummyHandler() {
		// �T�{��l��
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		//#1 ���`��DummyHandler
		MethodInvocation methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_printStackTrace_public", "e.printStackTrace()").get(0);
		dummyHandlerVisitor.detectDummyHandler(methodInvocation);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());

		//#2 ��throw
		methodInvocation = ASTNodeFinder
			.getMethodInvocationByMethodNameAndCode(compilationUnit,
			"false_throwAndPrint", "e.printStackTrace()").get(0);
		dummyHandlerVisitor.detectDummyHandler(methodInvocation);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
		
		//#3 �� Catch �~��
		methodInvocation = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
		"true_printStackTrace_protected", "fis.read()").get(0);
		dummyHandlerVisitor.detectDummyHandler(methodInvocation);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
	}

	/**
	 * �t�~���խY try statement ���D  try statement �����ɡA�O�_�|���T����
	 * @throws Exception 
	 */
	@Test
	public void testDetectDummyHandlerWithTryStatementInNonTryStatement() throws Exception {
		CompilationUnit compilationUnitWithTSINTS;
		
		// �s�إߴ��եΪ� DummyHandlerExampleWithTryStatementInNonTryStatement
		javaFile2String.clear();
		javaFile2String.read(DummyHandlerExampleWithTryStatementInNonTryStatement.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyHandlerExampleWithTryStatementInNonTryStatement.class.getPackage().getName(),
				DummyHandlerExampleWithTryStatementInNonTryStatement.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyHandlerExampleWithTryStatementInNonTryStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());

		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyHandlerExampleWithTryStatementInNonTryStatement.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
		compilationUnitWithTSINTS = (CompilationUnit) parser.createAST(null); 
		compilationUnitWithTSINTS.recordModifications();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnitWithTSINTS);

		// �T�{��l��
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		// do this test
		compilationUnitWithTSINTS.accept(dummyHandlerVisitor);
		
		// �����`�@���X��bad smell
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testDetectDummyHandlerWithNestedTryStatement() throws Exception {
		CompilationUnit compilationUnitWithTSINTS;
		
		// �s�إߴ��եΪ� DummyHandlerWithNestedTryStatement
		javaFile2String.clear();
		javaFile2String.read(DummyHandlerWithNestedTryStatement.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyHandlerWithNestedTryStatement.class.getPackage().getName(),
				DummyHandlerWithNestedTryStatement.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyHandlerWithNestedTryStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());

		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyHandlerWithNestedTryStatement.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
		compilationUnitWithTSINTS = (CompilationUnit) parser.createAST(null); 
		compilationUnitWithTSINTS.recordModifications();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnitWithTSINTS);

		// �T�{��l��
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		// do this test
		compilationUnitWithTSINTS.accept(dummyHandlerVisitor);
		
		// �����`�@���X��bad smell
		assertEquals(6, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_PrintStackTrace() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", MethodInvocation.class);
		method.setAccessible(true);
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		// ePrintStackTrace case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_ePrintStackTrace};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   ���O���աG�����Ҥl�B�ŦX�Ҥl�B���ŦX�Ҥl �O�_����X
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(10, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;
		mi = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(compilationUnit, "true_printStackTrace_protected", "e.printStackTrace()").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(11, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_SystemOutPrint() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", MethodInvocation.class);
		method.setAccessible(true);
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		dummyHandlerPatternsInXML = new String[] {
				SmellSettings.EXTRARULE_SystemErrPrint, SmellSettings.EXTRARULE_SystemErrPrintln,
				SmellSettings.EXTRARULE_SystemOutPrint, SmellSettings.EXTRARULE_SystemOutPrint};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   ���O���աG�����Ҥl�B�ŦX�Ҥl�B���ŦX�Ҥl �O�_����X
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(6, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;
		mi = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"true_systemTrace",
						"System.out.println(\"DummyHandlerExample.true_systemErrPrint()\")")
				.get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(7, dummyHandlerVisitor.getDummyList().size());

		
		mi = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_printStackTrace_protected",
				"e.printStackTrace()").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		
		assertEquals(7, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_JavaUtilLoggingLogger() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", MethodInvocation.class);
		method.setAccessible(true);
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		// JavaUtilLoggingLogger case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_JavaUtilLoggingLogger};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   ���O���աG�����Ҥl�B�ŦX�Ҥl�B���ŦX�Ҥl �O�_����X
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;
		mi = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_javaLogInfo",
				"javaLog.info(\"\")").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());

		mi = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_Log4J",
				"log4j.info(\"message\")").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());

	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_OrgApacheLog4j() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", MethodInvocation.class);
		method.setAccessible(true);
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		// OrgApacheLog4j case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_OrgApacheLog4j};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   ���O���աG�����Ҥl�B�ŦX�Ҥl�B���ŦX�Ҥl �O�_����X
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;
		mi = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(
				compilationUnit, "true_Log4J", "log4j.info(\"message\")")
				.get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
		
		mi = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_javaLogInfo",
				"javaLog.info(\"\")").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
	}

	/**
	 * ���շ��ϥΪ̦ۭq��type1�O�_����Outer class
	 * �����է���
	 */
	@Test
	public void testAddDummyHandlerSmellInfoForUserPatternType1() {
		String testClassPattern = UserDefineDummyHandlerFish.class.getName() + ".*";
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());

		// �T�{������Example���꦳������⦸�I�s
		smellSettings.addDummyHandlerPattern(testClassPattern, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
		
		// �T�{���ϥΪ̥��Ŀ�ɤ��|������
		setEmptySetting();
		smellSettings.addDummyHandlerPattern(testClassPattern, false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoWithUserDefinedPatternAndDetecting() {
		// �ϥΪ̦۩w�q *.toString ��Pattern�A�åB�n�D�n����
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;

		// �ŦX *.toString ���{���X -> e.toString()
		mi = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(
				compilationUnit, "true_systemOutPrintlnWithoutE",
				"e.toString()").get(0);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		dummyHandlerVisitor.detectDummyHandler(mi);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());

		// ���ŦX *.toString ���{���X -> e.toString.toCharArray()
		mi = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(
				compilationUnit, "true_systemOutPrintlnWithoutE",
				"e.toString().toCharArray()").get(0);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		dummyHandlerVisitor.detectDummyHandler(mi);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoWithUserDefinedPatternButNotDetecting() { 
		// �ϥΪ̦��۩w�q *.toString ��Pattern�A���O���n����
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString", false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		// �ϥΪ̿�J�אּ *.toString(), ���|�Q���P�O *.toString
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString()", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
	}

	/**
	 * ���շ��ϥΪ̦ۭq��type3�O�_����
	 * �P�ɴ���addDummyHandlerSmellInfo()���A�J��t��"<>"library�|�۰��ন"<"���e���l�r��
	 */
	@Test
	public void testAddDummyHandlerSmellInfoForUserPatternType3WithBracket() {
		String testClassPattern = "java.util.ArrayList.add";
		setEmptySetting();
		smellSettings.addDummyHandlerPattern(testClassPattern, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
	}

	@Test
	public void testIsThrowStatementInCatchClause() {
		TryStatement tryStatement;
		CatchClause catchStatement;

		// ���� �ŦX���Ҥl �O�_�|��X
		MethodDeclaration md = null;
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "false_throwAndPrint");
		tryStatement = (TryStatement) md.getBody().statements().get(0);
		catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		assertTrue(dummyHandlerVisitor.isThrowStatementInCatchClause(catchStatement));
		
		// ���� ���ŦX�Ҥl �O�_�|��X
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_DummyHandlerTryNestedTry");
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		assertFalse(dummyHandlerVisitor.isThrowStatementInCatchClause(catchStatement));
	}

	private void deleteOldSettings() {
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
	}
	
	private void setNewSettingsWithExtraRules(String[] argForXML) {
		deleteOldSettings();
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		for (int i=0; i<argForXML.length; i++) {
			smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, argForXML[i]);
		}
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private void setEmptySetting() {
		deleteOldSettings();
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}