from django.shortcuts import render
from django.conf import settings

import urllib2
import json

# Create your views here.


def home(request):

    def pagination(record_nmbr, bin_size):
        bins = record_nmbr / bin_size
        bin_resto = record_nmbr // bin_size
        bins_lst = []
        nlist = []
        for x in range(bins):
            nlist.append(x*bin_size)

        first = True
        for x in nlist[1:]:
            if first:
                bins_lst.append((0,x))
                first = False
                ant = x
            else:
                bins_lst.append((ant+1,x))
                ant = x
        return bins_lst
    
    countryd = {'ar':'Argentina', 'uy':'Uruguay'}
    
    if request.GET:
        cc = request.GET['country']
        country = countryd.get(cc,'Argentina')


        if 'from' in request.GET:
            from_ = int(request.GET['from'])
            to_ = int(request.GET['to'])
            bin_size = (to_-from_)+1
            requrl = settings.API_BASE_URL + 'places?filter={"where":{"Country":"%s"},"skip":%s,"limit":%s}'%(country, from_, bin_size)
            response = urllib2.urlopen(requrl)
            json_res = json.loads(response.read())
        else:
            bin_size = int(request.GET['points'])
            
            requrl = settings.API_BASE_URL + 'places?filter={"where":{"Country":"%s"},"limit":%s}'%(country, bin_size)
            response = urllib2.urlopen(requrl)
            json_res = json.loads(response.read())

        requrl = settings.API_BASE_URL + 'places/count/'
        response = urllib2.urlopen(requrl)
        record_nmbr = json.loads(response.read())['count']


        bins_lst = pagination(record_nmbr, bin_size)
        bins = len(bins_lst)
        # get next and previous
        # previous
        try:
            prev_from = from_ - bin_size
            prev_to = from_ - 1
            next_from = to_ + 1
            next_to = to_ + bin_size
        except:
            pass
            # Remove this


        context = {'data':json_res, 'country':country, 'bin_size':bin_size,
                   'bins':bins, 'bins_lst':bins_lst, 'cc':cc,
                   'prev_from': prev_from, 
                   'prev_to': prev_to,
                   'next_from':next_from,
                   'next_to':next_to,}

 

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