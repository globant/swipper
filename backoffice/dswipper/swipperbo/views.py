from django.shortcuts import render
from django.conf import settings

import urllib2
import json

# Create your views here.


def home(request):
    return render(request,"index.html")


def country(request,cc):

    countryd = {'ar':'Argentina', 'uy':'Uruguay'}
    country = countryd.get(cc,'Argentina')

    requrl = settings.API_BASE_URL + 'places?filter={"where":{"Country":"%s"},"limit":50}'%country

    response = urllib2.urlopen(requrl)
    json_res = json.loads(response.read())
    
    context = {'data':json_res, 'country':country}


    return render(request, "country.html", context)