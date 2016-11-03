import asyncio
import aiohttp
from rx import Observable
from rx.concurrency import AsyncIOScheduler
from rx import Observable

loop = asyncio.get_event_loop()
loop.set_debug(True)
scheduler = AsyncIOScheduler(loop)

def request(method, url, **kw):
    future = asyncio.ensure_future(aiohttp.request(method, url, **kw))
    return Observable.from_future(future)

ob = Observable.just(1, scheduler=scheduler)

ob10 = ob.\
    map(lambda x: request('GET', 'https://httpbin.org/')).\
    flat_map(lambda x: x)

ob10.subscribe(print)
ob.subscribe(print)

loop.run_forever()
