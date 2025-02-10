package art.aelaort.build;

import art.aelaort.exceptions.BuildJobNotFoundException;
import art.aelaort.models.build.Job;
import art.aelaort.utils.ExternalUtilities;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JobsProvider {
    private final ExternalUtilities externalUtilities;
    private final JsonMapper jsonMapper;

    public Job getJobById(int id) {
		return readBuildConfig()
				.stream()
				.filter(job -> job.getId() == id)
				.findFirst()
				.orElseThrow(BuildJobNotFoundException::new);
	}

    public Map<Integer, Job> getJobsMapById() {
        return readBuildConfig()
                .stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
    }

	public Map<String, Job> getJobsMapByName() {
		return readBuildConfig()
				.stream()
				.collect(Collectors.toMap(Job::getName, Function.identity(), (job, job2) -> job));
	}

    @SneakyThrows
    public List<Job> readBuildConfig() {
		String jobsStr = externalUtilities.readBuildConfig();
		Job[] jobs = jsonMapper.readValue(jobsStr, Job[].class);
		return Job.addNumbers(Arrays.asList(jobs));
	}
}
