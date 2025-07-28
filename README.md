# akka-job-orchestrator-ai
Project Proposal: AI-Powered Job Application Pipeline with Akka Cluster Sharding
Executive Summary
We're building a smart job application system that makes job hunting less painful and
more effective. Our platform uses Akka Cluster Sharding to run multiple AI agents that
work together—they research companies, customize resumes to match specific job
descriptions and company cultures, reach out to recruiters, and track where each
application stands. When you apply for a job, the system reads the job posting and
company profile, then tweaks your resume to highlight the most relevant skills and
experiences. It also keeps tabs on every application you've sent, reminds you to follow up,
and shows you real-time updates on your progress.
We're combining RAG technology with actor-based architecture to create something that's
both technically impressive and actually useful for job seekers.
Behind the scenes, we're using cluster sharding to distribute the workload—every job
application, company profile, and recruiter conversation runs as its own sharded entity.
This means the system can handle thousands of users without breaking a sweat, and if one
server goes down, the others pick up the slack without losing any data.
Team Composition
Project Type: Team Project (2 members)
Team Members:
Sachin Vishaul Baskar
Saurabh
