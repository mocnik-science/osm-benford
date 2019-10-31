#!/usr/bin/env python3

import colorcet as cc
import geopandas as gpd
import math
import matplotlib as mpl
import matplotlib.pyplot as plt
from mpl_toolkits.axes_grid1 import make_axes_locatable
import numpy as np
import os
import pandas as pd
import scipy.cluster.hierarchy as sch
import scipy.spatial.distance as scd
from shapely import wkt
import subprocess

output = './output'
outputPython = './output-python/'
outputWkt = './output-wkt/'

digits = np.arange(10)

latex = False
cmap = cc.cm.bmy.reversed()

fileNames = []
tableDefault = []
tableDefaultScale = []
dataGlobal = {}
isoGeometries = None

mpl.rcParams['axes.linewidth'] = .4

###### LOAD DATA ######

def loadIsoGeometries(simplification=.2):
    isoGeometries = None
    if isoGeometries is not None:
        return isoGeometries
    isos = []
    geometries = []
    for filename in sorted(os.listdir(outputWkt)):
        if filename.endswith('.wkt'):
            isos.append(filename[:-4])
            geometry = wkt.loads(pathlib.Path(os.path.join(outputWkt, filename)).read_text())
            geometry = geometry.simplify(simplification)
            geometries.append(geometry)
    isoGeometries = pd.Series(geometries, index=isos, name='geometry')
    return isoGeometries

###### PREPARE DATA ######

def computeForDistribution(absoluteDistribution, labelData, labelAspect, table=None, dataGlobal=dataGlobal, iso='global', scale=None, isBenford=False):
    if sum(absoluteDistribution[1:]) == 0:
        return
    relativeDistribution = [d / sum(absoluteDistribution[1:]) for d in absoluteDistribution]
    if scale is not None:
        if table is None:
            table = tableDefaultScale
        scaleParts = scale.split('_')
        computeStatistics(absoluteDistribution, relativeDistribution, table, labelData, labelAspect, scale=scaleParts[0], lon=scaleParts[1], lat=scaleParts[2])
    else:
        if table is None:
            table = tableDefault
        computeStatistics(absoluteDistribution, relativeDistribution, table, labelData, labelAspect, iso=iso, dataGlobal=dataGlobal)

def computeStatistics(absoluteDistribution, relativeDistribution, table, labelData, labelAspect, iso=None, scale=None, lon=None, lat=None, dataGlobal=None):
    if table is None:
        return
    bhattacharyya = - math.log(sum([relativeDistribution[i] * benford[i] for i in range(1, 10)]))
    result = {
        'bhattacharyya': bhattacharyya,
        'bhattacharyyaAngle': math.acos(sum([relativeDistribution[i] * benford[i] for i in range(1, 10)])),
        'kullbackLeiblerDivergence': - sum([relativeDistribution[i] * math.log(benford[i] / relativeDistribution[i]) if relativeDistribution[i] > 0 else 0 for i in range(1, 10)]),
        'hellingerDistance': 1 / math.sqrt(2) * math.sqrt(sum([(math.sqrt(relativeDistribution[i]) - math.sqrt(benford[i]))**2 for i in range(1, 10)])),
        'count': sum(absoluteDistribution),
        'labelData': labelData,
        'labelAspect': labelAspect,
    }
    if iso is not None:
        result['iso'] = iso
    if scale is not None:
        result['scale'] = scale
    if lon is not None:
        result['lon'] = lon
    if lat is not None:
        result['lat'] = lat
    table.append(result)
    if dataGlobal is None:
        return
    if labelData not in dataGlobal:
        dataGlobal[labelData] = {}
    if labelAspect not in dataGlobal[labelData]:
        dataGlobal[labelData][labelAspect] = [0 for _ in range(0, 10)]
    dataGlobal[labelData][labelAspect] = [dataGlobal[labelData][labelAspect][i] + absoluteDistribution[i] for i in range(0, 10)]

def prepareDataFrameRaw():
    return pd.DataFrame(tableDefault)

def prepareDataFrame():
    # pd.DataFrame(tableDefault).pivot_table(values='kullbackLeiblerDivergence', index='labelAspect', columns='iso')
    df = dfRaw.groupby(['labelData', 'labelAspect', 'iso'])['hellingerDistance'].aggregate('mean').unstack()
    # sort
    def sortLabelData(labelData, labelAspect):
        if labelData == 'peaks':
            return '_0'
        return labelData
    df['sort_labelData'] = [sortLabelData(*xs) for xs in df.index.values]
    df = df.sort_values(by=['labelAspect', 'sort_labelData'])
    df = df.drop(columns=['sort_labelData'])
    return df

def prepareDataFrameScale():
    dfScale = pd.DataFrame(tableDefaultScale)
    dfScale = dfScale.groupby(['lon', 'lat', 'labelData', 'labelAspect', 'scale'])['hellingerDistance'].aggregate('mean').unstack()
    # sort
    def sortLabelData(lon, lat, labelData, labelAspect):
        if labelData == 'peaks':
            return '_0'
        return labelData
    dfScale['sort_labelData'] = [sortLabelData(*xs) for xs in dfScale.index.values]
    dfScale = dfScale.sort_values(by=['lon', 'lat', 'labelAspect', 'sort_labelData'])
    dfScale = dfScale.drop(columns=['sort_labelData'])
    return dfScale

###### HELPING FUNCTIONS ######

def _locationToName(lon, lat):
    if lon == '6.885556' and lat == '52.223611':
        return 'ITC, Enschede, the Netherlands'
    if lon == '2.2945' and lat == '48.858222':
        return 'Eiffel Tower, Paris, France'
    if lon == '-73.977222' and lat == '40.761111':
        return 'The Museum of Modern Art, New York City, NY, USA'

def _plotSubDir(subdir):
    outputSubdir = os.path.join(output, subdir)
    if not os.path.isdir(outputSubdir):
        os.mkdir(outputSubdir)
    return outputSubdir

###### PLOT ######

def plotLegend():
    fig, ax = plt.subplots(figsize=(4, 2))
    n = 100
    m = np.zeros((n, 1))
    for i in range(n):
        m[i, 0] = i / (n - 1)
    ax.imshow(m, cmap=cmap, aspect=10/n)
    ax.yaxis.tick_right()
    ax.tick_params(width=.4)
    plt.xticks(np.arange(0))
    plt.yticks([0, n - 1], ['0 (high similarity)', '1 (low similarity)'])
    ax.set_ylim([0, n - 1])
    plt.savefig(os.path.join(output, 'legend' + '.pdf'))
    plt.close('all')

def plotLegendSimilarity():
    fig, ax = plt.subplots(figsize=(5, 2))
    n = 100
    m = np.zeros((n, 1))
    for i in range(n):
        m[i, 0] = i / (n - 1)
    ax.imshow(m, cmap=cmap, aspect=10/n)
    ax.yaxis.tick_right()
    ax.tick_params(width=.4)
    plt.xticks(np.arange(0))
    plt.yticks([0, n - 1], ['dissimilar or little deviation only\nfrom Benford\'s law', 'similar deviation\nfrom Benford\'s law'], fontsize=9)
    ax.set_ylim([0, n - 1])
    plt.savefig(os.path.join(output, 'legend-similarity' + '.pdf'))
    plt.close('all')

def plotLegendSimilarityHorizontal():
    fig, ax = plt.subplots(figsize=(3, 4))
    n = 100
    m = np.zeros((1, n))
    for i in range(n):
        m[0, i] = i / (n - 1)
    ax.imshow(m, cmap=cmap, aspect=n/10)
    ax.tick_params(width=.4)
    plt.yticks(np.arange(0))
    plt.xticks([0, n - 1], ['dissimilar\nor little', 'similar'])
    plt.xlabel('deviation from Benford\'s law', labelpad=6)
    ax.set_xlim([0, n - 1])
    plt.savefig(os.path.join(output, 'legend-similarity-horizontal' + '.pdf'))
    plt.close('all')

def plotDistributionHellingerVsKullbackLeibler():
    df2 = dfRaw.copy()[dfRaw.iso != 'global']
    # plot
    fig, ax = plt.subplots(figsize=(5, 5))
    ax.scatter(df2['kullbackLeiblerDivergence'], df2['hellingerDistance'], c='C0', marker='.', s=1, alpha=.4)
    ax.set_xlabel('Kullback-Leibler divergence')
    ax.set_ylabel('Hellinger distance')
    ax.margins(0)
    ax.set_ylim([0, 1])
    # save
    plt.savefig(os.path.join(output, 'distribution-hellinger-vs-kullback-leibler' + '.pdf'))
    plt.close('all')

def plotDistribution(relativeDistribution, labelData, labelAspect, iso=None, includeBenford=True, isBenford=False):
    if includeBenford and not isBenford:
        plotDistribution(benford, 'Benford\'s law', None, iso=iso, includeBenford=False)
    label = labelData + (' (' + labelAspect + ')' if labelAspect else '')
    baseLine, = plt.step(digits, relativeDistribution, where='mid', label=label)
    plt.plot(digits, relativeDistribution, 'o', alpha=.5, color=baseLine.get_color())
    if includeBenford:
        l = [label]
        if iso is not None:
            l.append(iso)
        plotLayout(' - '.join(l))
        filename = ' - '.join(['distribution', *l])
        plt.savefig(os.path.join(output, filename + '.pdf'))
        plt.close('all')
        fileNames.append(filename + '.pdf')

def plotLayout(label=''):
    if label == '':
        plt.legend() # title=
    else:
        plt.title(label)
    plt.xticks(digits)
    plt.yticks(np.arange(0, .45 + .05, .05))
    plt.xlabel('digits')
    plt.ylabel('frequency')

def plotDistributionPerCountry():
    # plot
    fig, ax = plt.subplots(figsize=(17, 17))
#    ax.imshow(df, cmap=cc.cm.fire.reversed())
    ax.imshow(df, cmap=cmap)
    ax.set_facecolor('#dddddd')
    # label formatting
    ax.xaxis.tick_top()
    ax.tick_params(axis='both', which='both', labelsize=5.5, bottom=False, top=False, left=False, right=False)
    ax.tick_params(axis='y', which='major', pad=120)
    ax.tick_params(axis='y', which='minor', pad=-.3)
    ax.tick_params(axis='x', rotation=90, pad=-1.5)
    # x labels
    ax.set_xticks(np.arange(len(df.columns)))
    ax.set_xticklabels(df.columns)
    ax.set_xlabel('countries')
    ax.xaxis.set_label_position('top')
    # y labels
    key1Ticks = []
    key1Labels = []
    key2Ticks = []
    key2Labels = []
    lastKey1 = None
    for i, ((key2, key1), _) in enumerate(df.iterrows()):
        if key1 != lastKey1:
            key1Ticks.append(i)
            key1Labels.append(key1)
            ax.axhline(y=-.5 + i, xmin=-.1315, xmax=1, linewidth=.4, color='black' if i == 0 else '#999999', clip_on=False, zorder=1)
            lastKey1 = key1
        key2Ticks.append(i)
        key2Labels.append(key2)
    ax.axhline(y=-.5 + i + 1, xmin=-.1315, xmax=1, linewidth=.4, color='black', clip_on=False, zorder=100)
    ax.set_yticks(key1Ticks)
    ax.set_yticklabels(key1Labels, ha='left')
    ax.set_yticks(key2Ticks, minor=True)
    ax.set_yticklabels(key2Labels, minor=True)
    # save
    plt.savefig(os.path.join(output, 'distribution-per-country' + '.pdf'))
    plt.close('all')

def plotDistributionHeterogeneityPerAspect():
    # data
    df2 = pd.DataFrame(data=None, columns=None, index=df.index)
    df2['stats_min'] = df.min(axis=1)
    df2['stats_quantile_lower'] = df.quantile(.25, axis=1)
    df2['stats_median'] = df.quantile(.5, axis=1)
    df2['stats_quantile_upper'] = df.quantile(.75, axis=1)
    df2['stats_max'] = df.max(axis=1)
    # plot
    fig, ax = plt.subplots(figsize=(16, 16))
    dfTmp = df2[::-1]
    ys = range(0, len(dfTmp.index))
    ax.scatter(dfTmp.stats_min, ys, s=51.5, marker='o', color='#1F77B4')
    ax.scatter(dfTmp.stats_median, ys, s=51.5, marker='o', color='#F27100')
    ax.scatter(dfTmp.stats_max, ys, s=51.5, marker='o', color='#1F77B4')
    for i, (_, d) in reversed(list(enumerate(dfTmp.iterrows()))):
        ax.add_line(mpl.lines.Line2D([d.stats_min, d.stats_max], [i, i], linewidth=8.2, color='#DCEDF9', solid_capstyle='round', zorder=0))
        ax.add_line(mpl.lines.Line2D([d.stats_quantile_lower, d.stats_quantile_upper], [i, i], linewidth=8.2, color='#FFBF87', solid_capstyle='round', zorder=0))
    # label formatting
    ax.xaxis.tick_top()
    ax.tick_params(axis='both', which='both', labelsize=7, bottom=True, top=False, left=False, right=False, labelbottom=True, labeltop=False)
    ax.tick_params(axis='y', which='major', pad=140, labelsize=7)
    ax.tick_params(axis='y', which='minor', pad=-.3, labelsize=7)
    # ax.autoscale(enable=True, axis='y', tight=True)
    ax.set_xlim([0, .91])
    ax.set_ylim([-.5, len(dfTmp.index) - 1 + .5])
    # x labels
    plt.xlabel('Hellinger distance', fontsize=13)
    # y labels
    key1Ticks = []
    key1Labels = []
    key2Ticks = []
    key2Labels = []
    lastKey1 = None
    for i, ((key2, key1), _) in reversed(list(enumerate(dfTmp.iterrows()))):
        if key1 != lastKey1:
            key1Ticks.append(i)
            key1Labels.append(key1)
            ax.axhline(y=.5 + i, xmin=-.1606, xmax=1, linewidth=.4, color='black' if i == len(dfTmp.index) - 1 else '#999999', clip_on=False, zorder=1)
            lastKey1 = key1
        key2Ticks.append(i)
        key2Labels.append(key2)
    ax.axhline(y=.5 + i - 1, xmin=-.1606, xmax=1, linewidth=.4, color='black', clip_on=False, zorder=100)
    ax.set_yticks(key1Ticks)
    ax.set_yticklabels(key1Labels, ha='left')
    ax.set_yticks(key2Ticks, minor=True)
    ax.set_yticklabels(key2Labels, minor=True)
    # save
    plt.savefig(os.path.join(output, 'distribution-heterogeneity-per-aspect' + '.pdf'))
    plt.close('all')

def plotDistributionHeterogeneityPerCountry():
    # data
    df2 = pd.DataFrame(data=None, columns=None, index=df.columns)
    df2['stats_min'] = df.min(axis=0)
    df2['stats_quantile_lower'] = df.quantile(.25, axis=0)
    df2['stats_median'] = df.quantile(.5, axis=0)
    df2['stats_quantile_upper'] = df.quantile(.75, axis=0)
    df2['stats_max'] = df.max(axis=0)
    # plot
    fig, ax = plt.subplots(figsize=(17, 17))
    dfTmp = df2.sort_values(by=['stats_median', 'stats_quantile_lower', 'stats_quantile_upper'])[::-1]
    ys = range(0, len(dfTmp.index))
    ax.scatter(dfTmp.stats_min, ys, s=9.7, marker='o', color='#1F77B4')
    ax.scatter(dfTmp.stats_median, ys, s=9.7, marker='o', color='#F27100')
    ax.scatter(dfTmp.stats_max, ys, s=9.7, marker='o', color='#1F77B4')
    for i, (_, d) in reversed(list(enumerate(dfTmp.iterrows()))):
        ax.add_line(mpl.lines.Line2D([d.stats_min, d.stats_max], [i, i], linewidth=4.19, color='#DCEDF9', solid_capstyle='round', zorder=0))
        ax.add_line(mpl.lines.Line2D([d.stats_quantile_lower, d.stats_quantile_upper], [i, i], linewidth=4.19, color='#FFBF87', solid_capstyle='round', zorder=0))
    # label formatting
    ax.xaxis.tick_top()
    ax.tick_params(axis='both', which='both', labelsize=7, bottom=True, top=False, left=False, right=False, labelbottom=True, labeltop=False)
    ax.tick_params(axis='y', pad=-.3)
    # ax.autoscale(enable=True, axis='y', tight=True)
    ax.set_xlim([0, .91])
    ax.set_ylim([-.5, len(dfTmp.index) - 1 + .5])
    # x labels
    plt.xlabel('Hellinger distance', fontsize=13)
    # y labels
    ax.set_yticks(ys)
    ax.set_yticklabels(dfTmp.index)
    # save
    plt.savefig(os.path.join(output, 'distribution-heterogeneity-per-country' + '.pdf'))
    plt.close('all')

def plotDistributionScaleDependency():
    locations = sorted(set([(xs[:2]) for xs in dfScale.index.values]))
    for labelData, labelAspect in set([(xs[2:4]) for xs in dfScale.index.values]):
        # plot
        fig, ax = plt.subplots()
        for p in locations:
            df2 = dfScale.loc[p].loc[(labelData, labelAspect)]
            ax.plot([2 * 111000 * 10**(-float(x)) for x in df2.index], df2.values, marker='o', label=_locationToName(*p))
        ax.legend()
        ax.invert_xaxis()
        ax.set_xscale('log')
        ax.set_ylim([0, .9])
        plt.xlabel('side length of the quadratic area in metres')
        plt.ylabel('Hellinger distance')
        # save
        plt.savefig(os.path.join(_plotSubDir('distribution-scale-dependency'), 'distribution-scale-dependency - ' + labelData + ' - ' + labelAspect + '.pdf'))
        plt.close('all')

def plotDistributionArealMap():
    isoGeometries = loadIsoGeometries()
    for ((labelData, labelAspect), _) in df.iterrows():
        df2 = gpd.GeoDataFrame(dict(value=df.loc[(labelData, labelAspect)], geometry=isoGeometries), geometry='geometry')
        df2 = df2.dropna(subset=['geometry'])
        df2.crs = {'init': 'epsg:4326'}
        df2 = df2.to_crs('+proj=cea +lon_0=0 +lat_ts=30 +x_0=0 +y_0=0 +datum=WGS84 +ellps=WGS84 +units=m +no_defs') # Behrmann - epsg:54017
        # plot
        fig, ax = plt.subplots()
        divider = make_axes_locatable(ax)
        cax = divider.append_axes('right', size='3%', pad=.1)
        df2.plot(column='value', cmap=cmap, vmin=0, vmax=.7, linewidth=.4, edgecolor='#ffffff', ax=ax, legend=True, cax=cax)
        ax.set_xticks([])
        ax.set_yticks([])
        ax.set_xlim(left=-17367530.445161372, right=17367530.445161372)
        ax.set_ylim(bottom=-7342230.136498678, top=7342230.13649868)
        ## computed using:
        ## from pyproj import Proj, transform
        ## inProj  = Proj("+init=EPSG:4326")
        ## outProj = Proj('+proj=cea +lon_0=0 +lat_ts=30 +x_0=0 +y_0=0 +datum=WGS84 +ellps=WGS84 +units=m +no_defs')
        ## transform(inProj, outProj, -180, -90)
        # save
        plt.savefig(os.path.join(_plotSubDir('distribution-areal-map'), 'distribution-areal-map - ' + labelData + ' - ' + labelAspect + '.pdf'))
        plt.close('all')

def plotDistributionCorrelationToCount():
    df2 = dfRaw.groupby(['labelData', 'labelAspect', 'iso']).mean().drop('global', level='iso')
    for labelData, labelAspect in set([(xs[:2]) for xs in df2.index.values]):
        df3 = df2.loc[(labelData, labelAspect)]
        # plot
        fig, ax = plt.subplots(figsize=(5, 5))
        ax.scatter(df3['count'], df3.hellingerDistance, facecolors='none', edgecolors='C0')
        ax.set_xlabel('number of elements')
        ax.set_ylabel('Hellinger distance')
        ax.ticklabel_format(style='sci', scilimits=(0, 0), useOffset=True, axis='x')
        ax.margins(0)
        ax.set_ylim([0, .9])
#        ax.set_title(labelData + ' - ' + labelAspect)
        # save
        plt.savefig(os.path.join(_plotSubDir('distribution-correlation-to-count'), 'distribution-correlation-to-count - ' + labelData + ' - ' + labelAspect + '.pdf'))
        plt.close('all')

thresholdHellingerDistance = .1
thresholdCount = .8

def plotDistributionExceptionCoincidenceAspects():
    df2 = df.copy().drop('global', axis=1)
    # filter for data and aspects that show strong similarities to the Benford distribution
    for labelData, labelAspect in set([(xs[:2]) for xs in df.index.values]):
        df3 = df.loc[(labelData, labelAspect)].drop('global')
        similarAbsolute = (df3 <= thresholdHellingerDistance).sum()
        similarRelative = similarAbsolute / df3.shape[0]
        if similarRelative < thresholdCount:
            df2 = df2.drop((labelData, labelAspect))
    # statistics
    df2 = df2.applymap(lambda x: x <= thresholdHellingerDistance)
    m = np.zeros((df2.shape[0], df2.shape[0]))
    for i1, (_, aspect1) in enumerate(df2.iterrows()):
        for i2, (_, aspect2) in enumerate(df2.iterrows()):
            for country in aspect1.index:
                if not aspect1[country] and not aspect2[country] and i1 != i2:
                    m[(i1, i2)] += 1
    # plot function
    def labels(ax, index):
        # label formatting
        ax.xaxis.tick_top()
        ax.tick_params(axis='both', which='both', labelsize=5.5, bottom=False, top=False, left=False, right=False)
        ax.tick_params(axis='x', which='major', pad=125, rotation=90)
        ax.tick_params(axis='x', which='minor', pad=0, rotation=90)
        ax.tick_params(axis='y', which='major', pad=125)
        ax.tick_params(axis='y', which='minor', pad=0)
        # labels
        ax.set_xticks(np.arange(len(index)))
        ax.set_xticklabels([key1 for (key1, _) in index], va='top')
        ax.set_xticks(np.arange(len(index)), minor=True)
        ax.set_xticklabels([key2 for (_, key2) in index], minor=True)
        ax.set_yticks(np.arange(len(index)))
        ax.set_yticklabels([key1 for (key1, _) in index], ha='left')
        ax.set_yticks(np.arange(len(index)), minor=True)
        ax.set_yticklabels([key2 for (_, key2) in index], minor=True)
    # plot
    m2 = np.copy(m)
    for i in range(0, m2.shape[1]):
        m2[(i, i)] = np.nan
    fig, ax = plt.subplots(figsize=(4, 4))
    fig.subplots_adjust(left=.5, top=.5)
    ax.imshow(m2, cmap=cmap)
    labels(ax, df2.index)
    # save
    plt.savefig(os.path.join(_plotSubDir('distribution-exception-coincidence-aspects'), 'distribution-exception-coincidence-aspects' + '.pdf'))
    plt.close('all')
    for method in ['single', 'complete', 'average', 'weighted', 'centroid', 'median', 'ward']:
        # sort
        m2, permutation = matrixBlockForm(m, method)
        for i in range(0, m2.shape[1]):
            m2[(i, i)] = np.nan
        # plot
        fig, ax = plt.subplots(figsize=(4, 4))
        fig.subplots_adjust(left=.5, top=.5)
        ax.imshow(m2, cmap=cmap)
        labels(ax, [df2.index[i] for i in permutation])
        # save
        plt.savefig(os.path.join(_plotSubDir('distribution-exception-coincidence-aspects'), 'distribution-exception-coincidence-aspects-sorted-' + method + '.pdf'))
        plt.close('all')

def plotDistributionExceptionCoincidenceCountries():
    df2 = df.copy().drop('global', axis=1)
    # filter for data and aspects that show strong similarities to the Benford distribution
    for labelData, labelAspect in set([(xs[:2]) for xs in df2.index.values]):
        df3 = df.loc[(labelData, labelAspect)].drop('global')
        similarAbsolute = (df3 <= thresholdHellingerDistance).sum()
        similarRelative = similarAbsolute / df3.shape[0]
        if similarRelative < thresholdCount:
            df2 = df2.drop((labelData, labelAspect))
    # statistics
    df2 = df2.applymap(lambda x: x <= thresholdHellingerDistance)
    m = np.zeros((df2.shape[1], df2.shape[1]))
    for i1, country1 in enumerate(df2.columns):
        for i2, country2 in enumerate(df2.columns):
            for aspect in df2.index:
                if not df2[country1][aspect] and not df2[country2][aspect] and i1 != i2:
                    m[(i1, i2)] += 1
    # plot function
    def labels(ax, index):
        # label formatting
        ax.xaxis.tick_top()
        ax.tick_params(axis='both', which='both', labelsize=5.5, bottom=False, top=False, left=False, right=False)
        ax.tick_params(axis='x', pad=0, rotation=90)
        ax.tick_params(axis='y', pad=0)
        # labels
        ax.set_xticks(np.arange(len(index)))
        ax.set_xticklabels(index)
        ax.set_yticks(np.arange(len(index)))
        ax.set_yticklabels(index)
    # plot
    m2 = np.copy(m)
    for i in range(0, m2.shape[1]):
        m2[(i, i)] = np.nan
    fig, ax = plt.subplots(figsize=(15, 15))
    ax.imshow(m2, cmap=cmap)
    labels(ax, df2.columns)
    # save
    plt.savefig(os.path.join(_plotSubDir('distribution-exception-coincidence-countries'), 'distribution-exception-coincidence-countries' + '.pdf'))
    plt.close('all')
    for method in ['single', 'complete', 'average', 'weighted', 'centroid', 'median', 'ward']:
        # sort
        m2, permutation = matrixBlockForm(m, method)
        for i in range(0, m2.shape[1]):
            m2[(i, i)] = np.nan
        # plot
        fig, ax = plt.subplots(figsize=(15, 15))
        ax.imshow(m2, cmap=cmap)
        labels(ax, [df2.columns[i] for i in permutation])
        # save
        plt.savefig(os.path.join(_plotSubDir('distribution-exception-coincidence-countries'), 'distribution-exception-coincidence-countries-sorted-' + method + '.pdf'))
        plt.close('all')

def matrixBlockForm(m, method='ward'):
    permutation = matrixBlockFormPermutation(m, method)
    m = np.array([m[p] for p in permutation])
    m = m.transpose()
    m = np.array([m[p] for p in permutation])
    m = m.transpose()
    return m, permutation

def matrixBlockFormPermutation(m, method):
    cluster, permutation = matrixBlockFormStep(m, method)
    if len(set(cluster)) > 1:
        permutation = matrixBlockFormPermutationSub(m, cluster, permutation, method)
    return permutation

def matrixBlockFormPermutationSub(m, cluster, permutation, method):
    unique, counts = np.unique(cluster, return_counts=True)
    nStart = 0
    for c, n in zip(unique, counts):
        if n > 1:
            js = permutation[nStart:nStart+n]
            mSub = m[js][:, js]
            permutationSub = matrixBlockFormPermutation(mSub, method)
            permutation = permutate(permutation, range(nStart, nStart + n), permutationSub)
        nStart += n
    return permutation

def matrixBlockFormStep(m, method):
    d = sch.distance.pdist(m)
    l = sch.linkage(d, method=method, optimal_ordering=False)
    cluster = sch.fcluster(l, .5 * d.max(), 'distance')
    return cluster, list(np.argsort(cluster))

def permutate(permutation, indices, permutationSub):
    permutationNew = permutation[:]
    for j, i in enumerate(indices):
        permutationNew[i] = permutation[indices[permutationSub[j]]]
    return permutationNew

###### DATA ######

import pathlib

exec(pathlib.Path(os.path.join(outputPython, 'benford.py')).read_text())
for filename in sorted(os.listdir(outputPython)):
    if filename.endswith('.py') and filename != 'benford.py':
        exec(pathlib.Path(os.path.join(outputPython, filename)).read_text())
for labelData in dataGlobal:
    for labelAspect in dataGlobal[labelData]:
        computeForDistribution(dataGlobal[labelData][labelAspect], labelData, labelAspect)

dfRaw = prepareDataFrameRaw()
df = prepareDataFrame()
dfScale = prepareDataFrameScale()

###### END ######

# pd.DataFrame(tableDefault).to_csv(os.path.join(output, 'table.csv'))
# plotLegend()
# plotLegendSimilarity()
# plotLegendSimilarityHorizontal()
# plotDistributionHellingerVsKullbackLeibler()
# plotDistributionPerCountry()
# plotDistributionHeterogeneityPerAspect()
# plotDistributionHeterogeneityPerCountry()
# plotDistributionScaleDependency()
# plotDistributionArealMap()
# plotDistributionCorrelationToCount()
# plotDistributionExceptionCoincidenceAspects()
# plotDistributionExceptionCoincidenceCountries()
# visualizations
if latex:
    f = open(os.path.join(output, 'visualizations.tex'), 'w')
    f.write('\\documentclass{article}\n')
    f.write('\\usepackage{graphicx}\n')
    f.write('\\usepackage[space]{grffile}\n')
    f.write('\\setlength\\parindent{0pt}\n')
    f.write('\\usepackage[margin=1.5cm]{geometry}\n')
    f.write('\\begin{document}\n')
    for filename in fileNames:
        f.write('\\includegraphics[width=.5\\textwidth]{' + filename + '}\n')
    f.write('\\end{document}\n')
    f.close()
    subprocess.run('pdflatex visualizations.tex', shell=True, cwd=output)
