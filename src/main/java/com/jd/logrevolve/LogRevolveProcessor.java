package com.jd.logrevolve;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(LogRevolve.CanonicalName)
public class LogRevolveProcessor extends AbstractProcessor {

    private LogRevolveProcessorUtil processorUtil;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processorUtil = new LogRevolveProcessorUtil(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || annotations.isEmpty()) {
            return false;
        }
        processorUtil.printNote("%s elements are marked by @%s.", annotations.size(), LogRevolve.CanonicalName);

        BlockOperator blockOperator = new BlockOperator();
        Set<? extends Element> markedElement = roundEnv.getElementsAnnotatedWith(LogRevolve.class);
        for (Element element : markedElement) {
            processorUtil.javacTrees.getTree(element).accept(blockOperator);
        }
        processorUtil.printNote("%s log statements are compiled.", blockOperator.getCount());
        return true;
    }

    private class BlockOperator extends TreeTranslator {

        private int count = 0;
        private java.util.List<JCTree.JCStatement> tmpStates = new ArrayList<>(16);

        @Override
        public void visitBlock(JCTree.JCBlock jcBlock) {
            boolean hasLog = false;
            // 遍历代码块中语句
            for (JCTree.JCStatement statement : jcBlock.getStatements()) {
                boolean minimumBlock = processorUtil.isMinimumBlock(statement);
                if (minimumBlock) {
                    // 识别log语句
                    LogIdentifyConfig.LogIdentify logIdentify = processorUtil.getLogIdentify(statement);
                    if (Objects.nonNull(logIdentify)) {
                        hasLog = true;
                        // log语句外套if
                        statement = processorUtil.packIfLogEnable(logIdentify, statement);
                        count ++;
                    }
                }
                tmpStates.add(statement);
            }
            if (hasLog) {
                // 打包新代码块
                jcBlock = processorUtil.toBlock(tmpStates);
            }
            tmpStates.clear();
            super.visitBlock(jcBlock);
        }

        public int getCount() {
            return this.count;
        }
    }

}
