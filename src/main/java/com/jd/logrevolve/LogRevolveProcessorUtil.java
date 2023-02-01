package com.jd.logrevolve;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.util.Objects;

public class LogRevolveProcessorUtil {
    final JavacTrees javacTrees;
    final TreeMaker treeMaker;
    final Names names;
    private final Messager messager;
    private LogStateJudge logStateJudge;
    private MinimumBlockJudge minimumBlockJudge;

    LogRevolveProcessorUtil(ProcessingEnvironment processingEnv) {
        this.javacTrees = JavacTrees.instance(processingEnv);
        this.messager = processingEnv.getMessager();
        JavacProcessingEnvironment javacProcessingEnvironment = (JavacProcessingEnvironment) processingEnv;
        Context context = javacProcessingEnvironment.getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
        logStateJudge = new LogStateJudge();
        minimumBlockJudge = new MinimumBlockJudge();
        LogIdentifyConfig.initFromFile();
        LogIdentifyConfig.print(this.messager);
    }

    public void printNote(String format, Object... arg) {
        this.messager.printMessage(Diagnostic.Kind.NOTE, String.format(format, arg));
    }

    public void printError(String format, Object... arg) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, String.format(format, arg));
    }

    public LogIdentifyConfig.LogIdentify getLogIdentify(JCTree.JCStatement state) {
        state.accept(logStateJudge);
        return logStateJudge.getLogIdentify();
    }

    public JCTree.JCStatement packIfLogEnable(LogIdentifyConfig.LogIdentify logIdentify, JCTree.JCStatement logState) {
        // 调用log.isXXXEnabled()作为if的条件
        JCTree.JCMethodInvocation conditionState = this.treeMaker.Apply(
                List.nil(),
                this.treeMaker.Select(
                        this.treeMaker.Ident(this.names.fromString(logIdentify.var)),
                        this.names.fromString(logIdentify.detMethod)
                ),
                List.nil()
        );
        // 生成if
        return this.treeMaker.If(conditionState, logState, null);
    }

    public JCTree.JCBlock toBlock(java.util.List<JCTree.JCStatement> states) {
        return treeMaker.Block(0L, List.from(states));
    }

    public boolean isMinimumBlock(JCTree.JCStatement state) {
        state.accept(minimumBlockJudge);
        return minimumBlockJudge.isMinimumBlock();
    }

    private class LogStateJudge extends TreeTranslator {


        private java.util.List<LogIdentifyConfig.LogIdentify> varIdentifiers = null;
        private java.util.List<LogIdentifyConfig.LogIdentify> methodIdentifiers = null;

        public LogIdentifyConfig.LogIdentify getLogIdentify() {
            LogIdentifyConfig.LogIdentify result = null;
            if (Objects.nonNull(varIdentifiers) && Objects.nonNull(methodIdentifiers)) {
                varIdentifiers.retainAll(methodIdentifiers);
                if (!varIdentifiers.isEmpty()) {
                    result = varIdentifiers.get(0);
                }
            }
            varIdentifiers = null;
            methodIdentifiers = null;
            return result;
        }

        @Override
        public void visitIdent(JCTree.JCIdent jcIdent) {
            super.visitIdent(jcIdent);
            java.util.List<LogIdentifyConfig.LogIdentify> varMatch = LogIdentifyConfig.match(jcIdent.getName().toString(), LogIdentifyConfig.MatchType.VAR_MATCH);
            if (Objects.nonNull(varMatch)) {
                varIdentifiers = varMatch;
            }
        }

        @Override
        public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation) {
            super.visitApply(jcMethodInvocation);
            JCTree.JCExpression methodSelect = jcMethodInvocation.getMethodSelect();
            if (Tree.Kind.MEMBER_SELECT.equals(methodSelect.getKind()) && methodSelect instanceof JCTree.JCFieldAccess) {
                String method = ((JCTree.JCFieldAccess) methodSelect).name.toString();
                java.util.List<LogIdentifyConfig.LogIdentify> methodMatch = LogIdentifyConfig.match(method, LogIdentifyConfig.MatchType.METHOD_MATCH);
                if (Objects.nonNull(methodMatch)) {
                    methodIdentifiers = methodMatch;
                }
            }
        }
    }

    private class MinimumBlockJudge extends TreeTranslator {

        private boolean isMinimumBlock = true;

        public boolean isMinimumBlock() {
            boolean tmp = this.isMinimumBlock;
            this.isMinimumBlock = true;
            return tmp;
        }

        @Override
        public void visitBlock(JCTree.JCBlock jcBlock) {
            super.visitBlock(jcBlock);
            this.isMinimumBlock = false;
        }
    }

}
