package com.jd.logrevolve;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class LogIdentifyConfig {

    public static final String FILE_DIC = "META-INF/log-revolve/";
    private static final String EXECUTION_PREFIX =  "#";
    private static final String SEPARATOR =  "\\.";
    private static final Set<LogIdentify> LOG_IDENTIFIES = new HashSet<>();
    private static final Set<LogIdentify> EXECUTION_IDENTIFIES = new HashSet<>();

    public synchronized static List<LogIdentify> match(String match, MatchType matchType) {
        List<LogIdentify> matchList = null;
        for (LogIdentify logIdentify : LOG_IDENTIFIES) {
            if (matchType.match(logIdentify, match)) {
                if (Objects.isNull(matchList)) {
                    matchList = new ArrayList<>();
                }
                matchList.add(logIdentify);
            }
        }
        return matchList;
    }

    private synchronized static void register(List<LogIdentify> logIdentifies) {
        if (Objects.isNull(logIdentifies)) {
            throw new IllegalArgumentException();
        }
        logIdentifies.forEach(logIdentify -> {
            if (logIdentify.var.startsWith(EXECUTION_PREFIX)) {
                LogIdentify execution = new LogIdentify(logIdentify.var.substring(1), logIdentify.method, logIdentify.detMethod);
                EXECUTION_IDENTIFIES.add(execution);
            } else {
                LOG_IDENTIFIES.add(logIdentify);
            }
        });
        LOG_IDENTIFIES.removeAll(EXECUTION_IDENTIFIES);
    }

    synchronized static void print(Messager messager) {
        for (LogIdentify logIdentify : LOG_IDENTIFIES) {
            messager.printMessage(Diagnostic.Kind.NOTE, String.format("logIdentify:%s.%s.%s", logIdentify.var, logIdentify.method, logIdentify.detMethod));
        }
    }

    synchronized static void initFromFile() {
        List<String> lines = new ArrayList<>();
        try {
            List<String> fromJar = getFromJar(FILE_DIC);
            lines.addAll(fromJar);
        } catch (Exception e) {}

        try {
            List<String> fromFile = getFromFile(FILE_DIC)
                    .stream()
                    .map(file -> FILE_DIC + file)
                    .map(LogIdentifyConfig::getFromFile)
                    .flatMap(list -> list.stream())
                    .collect(Collectors.toList());
            lines.addAll(fromFile);
        } catch (Exception e) {}

        List<LogIdentify> logIdentifies = lines.stream()
                .filter(line -> !isBlank(line))
                .map(String::trim)
                .distinct()
                .map(line -> line.split(SEPARATOR))
                .filter(arr ->
                        arr.length == 3
                                && !isBlank(arr[0])
                                && !isBlank(arr[1])
                                && !isBlank(arr[2])
                )
                .map(arr -> new LogIdentify(arr[0], arr[1], arr[2]))
                .collect(Collectors.toList());
        register(logIdentifies);
    }

    private static List<String> getFromFile(String name) {
        Enumeration<URL> resources;
        try {
            resources = LogIdentifyConfig.class.getClassLoader().getResources(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<String> allLines = new ArrayList<>();
        while (resources.hasMoreElements()) {
            try {
                List<String> lines = readStream(resources.nextElement().openStream());
                allLines.addAll(lines);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return allLines;
    }

    public static List<String> getFromJar(String dic) {
        String jarPath = LogIdentifyConfig.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            List<String> allLines = new ArrayList<>();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (!jarEntry.isDirectory() && jarEntry.getName().startsWith(dic)) {
                    allLines.addAll(readStream(jarFile.getInputStream(jarEntry)));
                }
            }
            return allLines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static List<String> readStream(InputStream inputStream) {
        if (Objects.isNull(inputStream)) {
            return new ArrayList<>(0);
        }
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            return bufferedReader.lines().collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (Objects.nonNull(bufferedReader)) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    interface MatchType {
        MatchType VAR_MATCH = (logIdentify, match) -> logIdentify.var.equals(match);
        MatchType METHOD_MATCH = (logIdentify, match) -> logIdentify.method.equals(match);
        MatchType DET_METHOD_MATCH = (logIdentify, match) -> logIdentify.detMethod.equals(match);

        boolean match(LogIdentify logIdentify, String match);
    }

    public static class LogIdentify {
        final String var;
        final String method;
        final String detMethod;

        public LogIdentify(String var, String method, String detMethod) {
            if (isBlank(var) || isBlank(method) || isBlank(detMethod)) {
                throw new IllegalArgumentException();
            }
            this.var = var;
            this.method = method;
            this.detMethod = detMethod;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LogIdentify that = (LogIdentify) o;
            return Objects.equals(var, that.var) && Objects.equals(method, that.method) && Objects.equals(detMethod, that.detMethod);
        }

        @Override
        public int hashCode() {
            return Objects.hash(var, method, detMethod);
        }
    }

    private LogIdentifyConfig() {}
}
