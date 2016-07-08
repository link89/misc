from celery import Celery

app = Celery('tasks', broker='redis://127.0.0.1:6379/0')

@app.task(bind=True)
def retry_demo(self, retries):
    print "task id: {}, retries: {}".format(self.request.id, self.request.retries)
    if self.request.retries >= retries:
        print "task id: {}, success".format(self.request.id)
        return True
    else:
        raise self.retry(countdown=1, max_retries=100)
