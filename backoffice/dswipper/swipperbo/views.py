from django.shortcuts import render
from django.conf import settings

import urllib2
import json

# Create your views here.


def home(request):
    if request.GET:
        cc = request.GET['country']
        size = request.GET['points']
        print size
        countryd = {'ar':'Argentina', 'uy':'Uruguay'}
        country = countryd.get(cc,'Argentina')

        requrl = settings.API_BASE_URL + 'places/count/'
        response = urllib2.urlopen(requrl)
        places_nmbr = json.loads(response.read())['count']
        requrl = settings.API_BASE_URL + 'places?filter={"where":{"Country":"%s"},"limit":%s}'%(country, size)

        response = urllib2.urlopen(requrl)
        json_res = json.loads(response.read())

        context = {'data':json_res, 'country':country, 'size':size}


        return render(request, "country.html", context)


    else:
        return render(request,"index.html")


def country(request,cc):



    # for pagination
    # Get ammount of records


    requrl = settings.API_BASE_URL + 'places?filter={"where":{"Country":"%s"},"limit":50}'%country

    response = urllib2.urlopen(requrl)
    json_res = json.loads(response.read())
    
    context = {'data':json_res, 'country':country}


    return render(request, "country.html", context)