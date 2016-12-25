#include <iostream>
#include <iomanip>
#include <chrono>
#include <thread>
#include <algorithm>
#include <stdlib.h>

// Toy C++ 11 Program: Stop Watch
// Compile: g++ stop_watch.cpp -std=c++11 -o stop_watch
// Run: ./stop_watch [-h <hour>] [-m <minute>] [-s <second>]

class StopWatch{
    private:
        int _hour;
        int _minute;
        int _second;

    public:
        StopWatch(int hour=0, int minute=0, int second=0):
            _hour(hour),
            _minute(minute),
            _second(second) {
        }

        StopWatch& operator++() {
            int carry = 1;

            _second += carry;
            carry = _second / 60;
            _second = _second % 60;

            _minute += carry;
            carry = _minute / 60;
            _minute = _minute % 60;

            _hour += carry;
            return *this;
        }

        StopWatch& operator++(int) {
            ++(*this);
            return *this;
        }

        friend void printTime(const StopWatch &sw);
};

void printTime(const StopWatch &sw) {
    std::cout
        << "\r"
        << std::setfill('0') << std::setw(2) << sw._hour   << ":"
        << std::setfill('0') << std::setw(2) << sw._minute << ":"
        << std::setfill('0') << std::setw(2) << sw._second << std::flush;
    return;
}

char* getCmdOption(char ** begin, char ** end, const std::string & option) {
    char ** itr = std::find(begin, end, option);
    if (itr != end && ++itr != end) {
        return *itr;
    }
    return 0;
}

int main(int argc, char* argv[]) {
    int hour = 0, minute = 0, second = 0;

    // parse command line option
    char *h = getCmdOption(argv, argv + argc, "-h");
    if (h) {
        hour = atoi(h);
    }
    char *m = getCmdOption(argv, argv + argc, "-m");
    if (m) {
        minute = atoi(m);
    }
    char *s = getCmdOption(argv, argv + argc, "-s");
    if (s) {
        second = atoi(s);
    }

    StopWatch sw(hour, minute, second);

    // start timer
    while(1) {
        printTime(sw);
        std::this_thread::sleep_for(std::chrono::seconds(1));
        sw++;
    }

    return 0;
}
