import os
import sys
import shutil

def loadBase():
    f = open('matchmaker/base.conf')
    s = f.read()
    f.close()
    return s

def loadMaps():
    return [f.split('.')[0] for f in os.listdir('maps') if os.path.isfile(os.path.join('maps',f))]

DEFAULT_MATCH_FILE = 'matchmaker/matches'
DEFAULT_OUT_FILE = 'matchmaker/output'
BASE = loadBase()
ALL_MAPS = loadMaps()

def resetDir(f):
    f = 'matchmaker/' + f
    if os.path.exists(f):
        shutil.rmtree(f)
    os.mkdir(f)

def clean():
    resetDir('replays')
    resetDir('logs')


def genConf(mapname, a, b, logname):
    conf = BASE % (mapname, a, b, 'matchmaker/replays/%s.rms' % logname)
    f = open('matchmaker/temp.conf','w')
    f.write(conf)
    f.close()

def runMatch(mapname, a, b, logname):
    print "Currently running match between %s and %s on map %s" % (a, b, mapname)
    genConf(mapname, a, b, logname)
    os.system('ant -f matchmaker/build.xml -Dconf=matchmaker/temp.conf > matchmaker/logs/%s.txt' % logname)
    os.remove('matchmaker/temp.conf')

def get(d, i, default=0):
    if i in d:
        return d[i]
    return default

def initDict():
    return {'tot' : 0, 'wins' : 0}

def analyzeMatch(log, stats, a, b, maps):
    f = open('matchmaker/logs/%s.txt' % log)
    won = False
    for line in f:
        if won:
            reason = line[line.index('[java]')+len('[java]'):].strip()
            break
        else:
            if '[java] [server]' in line:
                win = line[line.index('[java] [server]')+len('[java] [server]'):].strip()
                if 'wins' in win:
                    won = True
                    winTeam = win[:win.index('(')].strip()
                    loseTeam = a if winTeam == b else b
                    winSide = win[win.index(')')-1]
                    loseSide = 'A' if winSide == 'B' else 'B'
    f.close()

    print win

    winStats = stats[winTeam]
    winMaps = winStats['maps']
    loseStats = stats[loseTeam]
    loseMaps = loseStats['maps']

    winStats['tot'] = get(winStats, 'tot') + 1
    winStats['wins'] = get(winStats, 'wins') + 1
    winStats[winSide]['tot'] = get(winStats[winSide], 'tot') + 1
    winStats[winSide]['wins'] = get(winStats[winSide], 'wins') + 1
    winMaps[maps] = get(winMaps, maps, initDict())
    winMaps[maps]['tot'] = get(winMaps[maps], 'tot') + 1
    winMaps[maps]['wins'] = get(winMaps[maps], 'wins') + 1

    loseStats['tot'] = get(loseStats, 'tot') + 1
    loseStats[loseSide]['tot'] = get(loseStats[loseSide], 'tot') + 1
    loseMaps[maps] = get(loseMaps, maps, initDict())
    loseMaps[maps]['tot'] = get(loseMaps[maps], 'tot') + 1

    return '%s\n%s\n' % (win, reason)

def getWinrate(d):
    wins = get(d, 'wins')
    tot = get(d, 'tot')
    winrate = 0 if tot == 0 else 100 * float(wins)/tot
    return "%s wins / %s total games = %s winrate%%" % (wins, tot, winrate)

def main():
    if len(sys.argv) > 2:
        print "Too many arguments"
        sys.exit(0)

    outfile = DEFAULT_OUT_FILE if len(sys.argv) == 1 else sys.argv[1]
    matches = open(DEFAULT_MATCH_FILE)

    stats = {}

    a = []
    b = []
    mapname = []
    num = []

    for line in matches:
        if not line.strip():
            continue
        line = line.strip().split('|')
        a.append(line[0])
        b.append(line[1])
        if line[2] == '*':
            mapname.append(ALL_MAPS)
        else:
            mapname.append(line[2:])
    matches.close()

    for bot in set(a).union(set(b)):
        stats[bot] = initDict()
        stats[bot]['A'] = initDict()
        stats[bot]['B'] = initDict()
        stats[bot]['maps'] = {}

    allLog = ''

    for i in xrange(len(a)):
        print "Running matches between %s and %s" % (a[i], b[i])
        for maps in mapname[i]:
            log = '%s_vs_%s_%s' % (a[i], b[i], maps)
            runMatch(maps, a[i], b[i], log)
            allLog += "\n%s vs %s on map %s\n" % (a[i], b[i], maps)
            allLog += analyzeMatch(log, stats, a[i], b[i], maps)

    logHeader = 'Log of the results of all matches\n=========================\n'

    for bot in stats:
        logHeader += '-------------------------\nBot %s:\n' % bot
        logHeader += "Overall: %s\n" % getWinrate(stats[bot])
        logHeader += "Team A: %s\n" % getWinrate(stats[bot]['A'])
        logHeader += "Team B: %s\n" % getWinrate(stats[bot]['B'])
        for maps in stats[bot]['maps']:
            logHeader += "Map %s: %s\n" % (maps, getWinrate(stats[bot]['maps'][maps]))

    print logHeader
    print "See output file for full log"

    f = open(outfile, 'w')
    f.write(logHeader + allLog)
    f.close()

clean()
main()