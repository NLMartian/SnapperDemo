#include <string.h>
#include <sys/types.h>
#include <dirent.h>
#include <stdio.h>

int watch(char *pidnum)
{
    char proc_path[100];
    DIR *proc_dir;

    // 初始化进程目录
    strcpy(proc_path, "/proc/");
    strcat(proc_path, pidnum);
    strcat(proc_path, "/");
    printf("proccess path is %s\n", proc_path);

    // 尝试访问它以确定是否存在
    proc_dir = opendir(proc_path);

    if (proc_dir == NULL)
    {
        printf("process: %s not exist! start it!\n", pidnum);
        return 1;
    }
    else
    {
        printf("process: %s exist!\n", pidnum);
        return 0;
    }
}

void restart_target()
{
    char cmd_restart[128] = "am startservice --user 0 -a me.nlmartian.android.snaperandroid.MessageService2";
    popen(cmd_restart, "r");
}

int main(int argc, char *argv[])
{
	char *pid = argv[1];
	while(1)
	{
	    if (watch(pid))
	    {
	        // 如果监听不到目标进程，则启动它
	        restart_target();
	        break;
	    }

	    sleep(2);
	}
	return 0;
}