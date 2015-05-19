from django.shortcuts import render
from django.conf import settings
from django.views.decorators.csrf import ensure_csrf_cookie
from django.http import JsonResponse


import json
import requests
import pdb
# Create your views here.

countryd = {'ar':'Argentina', 'uy':'Uruguay'}

def delete(request):
    if request.method == "POST":
        json_data = json.loads(request.body)
        try:
            id_for_delete = json_data['deleteid']
            #print data
        except KeyError:
            HttpResponseServerError("Malformed data!")
        # make request to delete data
        r = requests.delete(settings.API_BASE_URL + 'places/%s'%id_for_delete)
        if r.status_code == 204:
            return JsonResponse({'result':'OK'})
        else:
            return JsonResponse({'result':r.status_code})

def update(request):
    if request.method == "POST":
        json_data = json.loads(request.body)
        # get id from url
        id_ = request.path.split('/')[2]

    location = {}
    location['lat'] = json_data.pop('lat')
    location['lng'] = json_data.pop('lon')
    json_data['Location'] = json.dumps(location)
    json_data['Country'] = countryd[json_data.pop('Country')]
    
    r = requests.post(settings.API_BASE_URL + 'places/update?where={"id":"%s"}'%id_,
        data=json_data)
    if r.status_code == 204:
        return JsonResponse({'result':'OK'})


def addnew(request):
    if request.method == "POST":
        json_data = json.loads(request.body)
        print json_data
    try:
        name = json_data['Name']
        address = json_data['Address']
        phone = json_data['Phone']
        location = json_data['Location']
        category = json_data['Category']
        city = json_data['City']
    except KeyError:
        HttpResponseServerError("Malformed data!")
    
    # just for test
    # Check to have only required data
    r = requests.post(settings.API_BASE_URL + 'places/',
                        data=json_data)
    if r.status_code == 200:
        return JsonResponse({'result':'OK'})



def home(request):

    def _pagination(record_nmbr, bin_size):
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
    
    
    if request.GET:
        cc = request.GET['country']
        country = countryd.get(cc,'Argentina')


        if 'from' in request.GET:
            from_ = int(request.GET['from'])
            to_ = int(request.GET['to'])
            bin_size = (to_-from_)+1
            requrl = settings.API_BASE_URL + 'places?filter={"where":{"Country":"%s"},"skip":%s,"limit":%s}'%(country, from_, bin_size)
            response = requests.get(requrl)
            json_res = response.json
            prev_from = from_ - bin_size
            prev_to = from_ - 1
            next_from = to_ + 1
            next_to = to_ + bin_size
            record_nmbr = int(request.GET['record_nmbr'])
        else:
            bin_size = int(request.GET['points'])
            requrl = settings.API_BASE_URL + 'places/count/'
            response = requests.get(requrl)
            record_nmbr = response.json()['count'] 
            
            requrl = settings.API_BASE_URL + 'places?filter={"where":{"Country":"%s"},"limit":%s}'%(country, bin_size)
            response = requests.get(requrl)
            json_res = response.json
            prev_from = 0
            prev_to = 0
            next_from = bin_size + 1
            next_to = record_nmbr



        bins_lst = _pagination(record_nmbr, bin_size)
        bins = len(bins_lst)
        # get next and previous
        # previous

        context = {'data':json_res, 'country':country, 'bin_size':bin_size,
                   'bins':bins, 'bins_lst':bins_lst, 'cc':cc,
                   'prev_from': prev_from, 
                   'prev_to': prev_to,
                   'next_from':next_from,
                   'next_to':next_to,
                   'record_nmbr':record_nmbr,
                   }

 

        return render(request, "country.html", context)


    else:
        return render(request,"index.html")

@ensure_csrf_cookie
def country(request,cc):

    # WARNING: NO USED NOW!!!
    # for pagination
    # Get ammount of records

    requrl = settings.API_BASE_URL + 'places?filter={"where":{"Country":"%s"},"limit":50}'%country
    response = requests.get(requrl)
    json_res = response.json    
    context = {'data':json_res, 'country':country}


    return render(request, "country.html", context)