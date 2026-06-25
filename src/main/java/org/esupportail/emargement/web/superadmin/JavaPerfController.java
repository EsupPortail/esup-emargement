package org.esupportail.emargement.web.superadmin;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.esupportail.emargement.services.UnusedColumnDetectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.pool2.factory.PooledContextSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.zaxxer.hikari.HikariDataSource;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class JavaPerfController {
	
	private final static String ITEM = "javaperf";
	
	@Resource
	UnusedColumnDetectorService detector;

	@Resource
	DataSource basicDataSources;
	
	@Autowired
	private LdapContextSource ldapContextSource;

	@Autowired(required = false)
	List<PooledContextSource> poolingContextSources = new ArrayList<>();
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/superadmin/javaperf")
	public String getJavaPerf(Model uiModel) {

		Runtime runtime = Runtime.getRuntime();
		long maxMemoryInMB = runtime.maxMemory() / 1024 / 1024;
		long totalMemoryInMB = runtime.totalMemory() / 1024 / 1024;
		long freeMemoryInMB = runtime.freeMemory() / 1024 / 1024;
		long usedMemoryInMB = totalMemoryInMB - freeMemoryInMB;
		uiModel.addAttribute("maxMemoryInMB", maxMemoryInMB);
		uiModel.addAttribute("totalMemoryInMB", totalMemoryInMB);
		uiModel.addAttribute("freeMemoryInMB", freeMemoryInMB);
		uiModel.addAttribute("usedMemoryInMB", usedMemoryInMB);
		
		HikariDataSource hikariDS = null;
		try {
		    if (basicDataSources instanceof HikariDataSource) {
		        hikariDS = (HikariDataSource) basicDataSources;
		    } else if (basicDataSources.isWrapperFor(HikariDataSource.class)) {
		        hikariDS = basicDataSources.unwrap(HikariDataSource.class);
		    }
		} catch (Exception ignored) {}

		uiModel.addAttribute("ds", hikariDS);

		uiModel.addAttribute("ldapContextSource", ldapContextSource);

		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		List<ThreadInfo> threadInfos = Arrays.asList(threadMXBean.dumpAllThreads(true, true));
		Collections.sort(threadInfos, (ThreadInfo o1, ThreadInfo o2) -> o1.getThreadState().compareTo(o2.getThreadState()));
		Map<String, Long> threadStateCount = new HashMap<>();
		for (ThreadInfo threadInfo : threadInfos) {
			String threadState = threadInfo.getThreadState().toString();
			if (threadStateCount.containsKey(threadState)) {
				threadStateCount.put(threadState, threadStateCount.get(threadState) + 1);
			} else {
				threadStateCount.put(threadState, 1L);
			}
		}
		uiModel.addAttribute("threadMXBean", threadMXBean);
		uiModel.addAttribute("threadInfos", threadInfos);
		uiModel.addAttribute("threadStateCount", threadStateCount);
		long currentThreadId = Thread.currentThread().getId();
		uiModel.addAttribute("currentThreadId", currentThreadId);

        uiModel.addAttribute("javaPerfWrapper", new JavaPerfWrapper(threadMXBean, threadInfos));
     // Controller
        List<List<String>> rows = new ArrayList<>();
        detector.findUnusedColumns().forEach((table, cols) -> {
            cols.forEach(col -> {
                rows.add(Arrays.asList(table, col));
            });
        });
        uiModel.addAttribute("unusedColumns", rows);
        return "superadmin/javaperf";
	}


    class JavaPerfWrapper {
        private final ThreadMXBean threadMXBean;
        private final List<ThreadInfo> threadInfos;
        public JavaPerfWrapper(ThreadMXBean threadMXBean, List<ThreadInfo> threadInfos) {
            this.threadMXBean = threadMXBean;
            this.threadInfos = threadInfos;
        }
        public ThreadMXBean getThreadMXBean() {
            return threadMXBean;
        }
        public List<ThreadInfo> getThreadInfos() {
            return threadInfos;
        }
        public int getThreadCount() {
            return threadMXBean.getThreadCount();
        }
        public int getPeakThreadCount() {
            return threadMXBean.getPeakThreadCount();
        }
        public List<ThreadInfoWrapper> getThreadInfoWrappers() {
            List<ThreadInfoWrapper> wrappers = new ArrayList<>();
            for (ThreadInfo threadInfo : threadInfos) {
                wrappers.add(new ThreadInfoWrapper(threadInfo));
            }
            return wrappers;
        }
    }

    /*
    Permet d'accéder aux informations d'un ThreadInfo via thymeleaf
                threadId
               threadName
               threadState
               blockedCount
               blockedTime
               waitedCount
               waitedTime
               stackTrace
     */
    class ThreadInfoWrapper {
        private final ThreadInfo threadInfo;
        public ThreadInfoWrapper(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }
        public long getThreadId() {
            return threadInfo.getThreadId();
        }
        public String getThreadName() {
            return threadInfo.getThreadName();
        }
        public String getThreadState() {
            return threadInfo.getThreadState().name();
        }
        public long getBlockedCount() {
            return threadInfo.getBlockedCount();
        }
        public long getBlockedTime() {
            return threadInfo.getBlockedTime();
        }
        public long getWaitedCount() {
            return threadInfo.getWaitedCount();
        }
        public long getWaitedTime() {
            return threadInfo.getWaitedTime();
        }
        public StackTraceElementWrapper[] getStackTrace() {
            StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
            StackTraceElementWrapper[] wrappers = new StackTraceElementWrapper[stackTraceElements.length];
            for (int i = 0; i < stackTraceElements.length; i++) {
                wrappers[i] = new StackTraceElementWrapper(stackTraceElements[i]);
            }
            return wrappers;
        }
    }

    class StackTraceElementWrapper {
        private final StackTraceElement stackTraceElement;

        public StackTraceElementWrapper(StackTraceElement stackTraceElement) {
            this.stackTraceElement = stackTraceElement;
        }

        public String getClassName() {
            return stackTraceElement.getClassName();
        }

        public String getMethodName() {
            return stackTraceElement.getMethodName();
        }

        public String getFileName() {
            return stackTraceElement.getFileName();
        }

        public int getLineNumber() {
            return stackTraceElement.getLineNumber();
        }

        public String getToString() {
            return stackTraceElement.toString();
        }
    }
}
